# CityBooking dev environment launcher (Windows / PowerShell)
# Starts or reuses a Docker MySQL(8) container, waits until ready,
# then launches the Spring Boot backend in the foreground (Ctrl+C to stop).
# MySQL container persists across runs; the backend connects via the dev profile.

$ErrorActionPreference = "Stop"

$CONTAINER            = "mysql-cb"
$DB_PORT             = "3306"
$MYSQL_ROOT_PASSWORD = "root"
$DB_NAME             = "citybooking_dev"
$MYSQL_IMAGE         = "mysql:8"
$SERVER_DIR          = Join-Path $PSScriptRoot "server"

# ---------- 1. Check Docker ----------
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "docker not found. Install Docker Desktop and start it first."
    exit 1
}
try {
    docker info *> $null
} catch {
    Write-Error "Docker daemon is not running. Start Docker Desktop first."
    exit 1
}

# ---------- 2. Create / start MySQL container ----------
$exists = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $CONTAINER }
if (-not $exists) {
    Write-Host "==> Creating and starting MySQL container [$CONTAINER] (first run pulls the image) ..."
    docker run -d --name $CONTAINER `
        -e "MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD" `
        -e "MYSQL_DATABASE=$DB_NAME" `
        -p "${DB_PORT}:3306" `
        $MYSQL_IMAGE
} else {
    $running = docker ps --format "{{.Names}}" | Where-Object { $_ -eq $CONTAINER }
    if (-not $running) {
        Write-Host "==> Starting existing container [$CONTAINER] ..."
        docker start $CONTAINER
    } else {
        Write-Host "==> MySQL container [$CONTAINER] is already running."
    }
}

# ---------- 3. Wait for MySQL to be ready ----------
Write-Host "==> Waiting for MySQL to be ready (up to 60s) ..."
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    # Wrap with cmd /c so mysqladmin's stderr warning is swallowed by cmd (2>nul),
    # avoiding PowerShell's NativeCommandError under $ErrorActionPreference="Stop".
    $out = cmd /c "docker exec $CONTAINER mysqladmin ping -h localhost -uroot -p$MYSQL_ROOT_PASSWORD 2>nul"
    if ($LASTEXITCODE -eq 0 -and $out -like "*alive*") { $ready = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $ready) {
    Write-Error "MySQL was not ready in time. Check logs: docker logs $CONTAINER"
    exit 1
}
Write-Host "==> MySQL is ready (localhost:$DB_PORT / db $DB_NAME / user root)."

# ---------- 4. Pick a Maven command ----------
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    $mvnCmd = "mvn"
} else {
    $mvnw = Join-Path $SERVER_DIR "mvnw.cmd"
    if (Test-Path $mvnw) { $mvnCmd = ".\mvnw.cmd" }
    else { Write-Error "Neither mvn nor mvnw found. Install Maven or keep mvnw in the project."; exit 1 }
}

# ---------- 4.5 Free app ports (stop leftover backend holding target jar / port) ----------
foreach ($p in @(8080, 18100)) {
    $lp = (Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue).OwningProcess | Select-Object -First 1
    if ($lp) {
        Write-Host "==> Freeing port $p (stopping pid $lp) ..."
        Stop-Process -Id $lp -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
}

# ---------- 5. Build & launch backend (foreground; Ctrl+C to stop) ----------
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "java not found on PATH. Install a JDK (17+)."
    exit 1
}
Write-Host "==> Building backend (clean package, tests skipped) ..."
Set-Location $SERVER_DIR
& $mvnCmd clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Backend build failed. See Maven output above."
    exit 1
}
$jar = Get-ChildItem target\*.jar | Where-Object { $_.Name -notlike "*.original" } | Select-Object -First 1
Write-Host "==> Launching backend jar (dev profile -> MySQL): $($jar.Name) ..."
java -jar $jar.FullName
