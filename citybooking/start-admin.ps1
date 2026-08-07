<#
.SYNOPSIS
  CityBooking 管理端一键启动脚本（Windows / PowerShell 7+，兼容 Windows PowerShell 5.1）
  启动链路：MySQL(Docker) -> 后端 Spring Boot(:18100) -> 前端 Vite(:5173)
  管理端访问：http://localhost:5173  ->  /admin （已初始化超管种子账号）

.PARAMETER Build
  强制重新 `mvn package` 后端（确保管理端后端接口为最新代码）。

.PARAMETER NoBuild
  跳过后端构建，直接启动已有 jar（快速重启）。若 jar 不存在则报错退出。

.PARAMETER NoDB
  跳过 MySQL 容器管理（当你已在使用外部 MySQL 时）。

.PARAMETER NoBackend
  仅启动前端（调试纯前端时使用）。

.PARAMETER Test
  自检模式：启动 -> 等待端口就绪 -> 打印健康检查 -> 自动停止并退出。
  用于 CI / 自动验证“能否启动”。
#>
[CmdletBinding()]
param(
  [switch]$Build,
  [switch]$NoBuild,
  [switch]$NoDB,
  [switch]$NoBackend,
  [switch]$Test
)

$ErrorActionPreference = 'Stop'
$root       = $PSScriptRoot
$serverDir  = Join-Path $root 'server'
$webDir     = Join-Path $root 'web'

$CONTAINER  = 'mysql-cb'
$DB_PORT    = 3306
$DB_NAME    = 'citybooking_dev'
$MYSQL_PWD  = 'root'
$BE_PORT    = 18100
$FE_PORT    = 5173

$backendLog  = Join-Path $root 'backend.out.log'
$backendErr  = Join-Path $root 'backend.err.log'
$frontendLog = Join-Path $root 'frontend.out.log'
$frontendErr = Join-Path $root 'frontend.err.log'

$script:backendProc  = $null
$script:frontendProc = $null

function Write-Step($m) { Write-Host "`n==> $m" -ForegroundColor Cyan }
function Need($c) {
  if (-not (Get-Command $c -ErrorAction SilentlyContinue)) {
    Write-Error "$c not found on PATH. Please install it first."
    exit 1
  }
}

function Free-Port($port) {
  $pids = (Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue).OwningProcess |
    Where-Object { $_ -and $_ -ne $PID } | Select-Object -Unique
  foreach ($p in $pids) {
    try { Stop-Process -Id $p -Force -ErrorAction Stop; Write-Host "   freed port $port (killed pid $p)" }
    catch { Write-Warning "   could not kill pid $p on port $port" }
  }
}

function Wait-Tcp($port, $timeout = 180) {
  $t = 0
  while ($t -lt $timeout) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { return $true }
    Start-Sleep -Seconds 2; $t += 2
  }
  return $false
}

function Start-MySQL {
  if ($NoDB) { Write-Host '   (--NoDB) skip MySQL container management'; return }
  Need docker
  $exists = docker ps -a --format '{{.Names}}' | Where-Object { $_ -eq $CONTAINER }
  if (-not $exists) {
    Write-Step "Creating MySQL container [$CONTAINER] (first run pulls mysql:8)"
    docker run -d --name $CONTAINER `
      -e "MYSQL_ROOT_PASSWORD=$MYSQL_PWD" `
      -e "MYSQL_DATABASE=$DB_NAME" `
      -p "${DB_PORT}:3306" mysql:8
    if ($LASTEXITCODE -ne 0) { Write-Error 'Failed to create MySQL container.'; exit 1 }
  } else {
    $running = docker ps --format '{{.Names}}' | Where-Object { $_ -eq $CONTAINER }
    if (-not $running) { Write-Host "   starting existing container [$CONTAINER]"; docker start $CONTAINER | Out-Null }
    else { Write-Host '   MySQL container already running' }
  }
  Write-Host '   waiting for MySQL ready (up to 60s)...'
  $ok = $false
  for ($i = 0; $i -lt 30; $i++) {
    $out = cmd /c "docker exec $CONTAINER mysqladmin ping -h localhost -uroot -p$MYSQL_PWD 2>nul"
    if ($LASTEXITCODE -eq 0 -and $out -like '*alive*') { $ok = $true; break }
    Start-Sleep -Seconds 2
  }
  if (-not $ok) { Write-Error "MySQL not ready in time. Check: docker logs $CONTAINER"; exit 1 }
  Write-Host '   MySQL ready.'
}

function Start-Backend {
  if ($NoBackend) { Write-Host '   (--NoBackend) skip backend'; return }
  Need java
  $jar = Get-ChildItem (Join-Path $serverDir 'target\*.jar') -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1

  if ($NoBuild) {
    if (-not $jar) { Write-Error 'No backend jar found; cannot use --NoBuild. Run without --NoBuild to build it.'; exit 1 }
    Write-Host "   reuse existing jar: $($jar.Name)"
  } else {
    Write-Step 'Building backend (mvn clean package -DskipTests)...'
    Push-Location $serverDir
    try { & mvn clean package -DskipTests } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { Write-Error 'Backend build failed. See Maven output above.'; exit 1 }
    $jar = Get-ChildItem (Join-Path $serverDir 'target\*.jar') -ErrorAction SilentlyContinue |
      Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    if (-not $jar) { Write-Error 'Built jar not found in server/target/. Build may have failed.'; exit 1 }
  }

  Free-Port $BE_PORT
  Write-Step "Launching backend jar on :$BE_PORT (dev profile -> MySQL): $($jar.Name)"
  $script:backendProc = Start-Process -FilePath 'java' -ArgumentList '-jar', $jar.FullName `
    -WorkingDirectory $serverDir -RedirectStandardOutput $backendLog -RedirectStandardError $backendErr -PassThru
}

function Start-Frontend {
  Need node; Need npm
  if (-not (Test-Path (Join-Path $webDir 'node_modules'))) {
    Write-Step 'Installing frontend dependencies (npm install)...'
    Push-Location $webDir
    try { & npm install } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { Write-Error 'npm install failed.'; exit 1 }
  }
  Free-Port $FE_PORT
  Write-Step "Launching frontend (vite dev) on :$FE_PORT"
  # Windows 上 npm 实为 npm.cmd，Start-Process 无法直接执行，用 cmd /c 包装以便后台运行
  $script:frontendProc = Start-Process -FilePath 'cmd' -ArgumentList '/c', 'npm run dev' `
    -WorkingDirectory $webDir -RedirectStandardOutput $frontendLog -RedirectStandardError $frontendErr -PassThru
}

function Stop-All {
  Write-Host "`n==> Stopping managed processes..." -ForegroundColor Yellow
  if ($script:backendProc) {
    try { Stop-Process -Id $script:backendProc.Id -Force -ErrorAction Stop; Write-Host '   backend stopped' }
    catch { Write-Warning '   backend already gone' }
  }
  if ($script:frontendProc) {
    try { Stop-Process -Id $script:frontendProc.Id -Force -ErrorAction Stop; Write-Host '   frontend stopped' }
    catch { Write-Warning '   frontend already gone' }
  }
  Free-Port $BE_PORT
  Free-Port $FE_PORT
}

trap { Stop-All; break }

# ---------------- main ----------------
Write-Host 'CityBooking 管理端启动脚本 (admin launcher)' -ForegroundColor Green
Start-MySQL
Start-Backend
Start-Frontend

Write-Step 'Waiting for services to become ready...'
$beOk = if ($NoBackend) { $true } else { Wait-Tcp $BE_PORT 240 }
$feOk = Wait-Tcp $FE_PORT 120

if (-not $feOk) {
  Write-Error "Frontend (vite) did not start listening on :$FE_PORT. See frontend.out.log / frontend.err.log"
  Stop-All; exit 1
}
if (-not $NoBackend -and -not $beOk) {
  Write-Error "Backend did not start listening on :$BE_PORT. See backend.out.log / backend.err.log"
  Stop-All; exit 1
}

Write-Host "`n=============================================" -ForegroundColor Green
Write-Host '  管理端已启动 / Admin panel is UP' -ForegroundColor Green
Write-Host "  前端(管理端):  http://localhost:$FE_PORT" -ForegroundColor White
Write-Host "  后端 API:      http://localhost:$BE_PORT/api" -ForegroundColor White
Write-Host "  管理端登录:    http://localhost:$FE_PORT/admin" -ForegroundColor White
Write-Host '  超管账号(种子): 10000000000 / Admin@123456' -ForegroundColor White
Write-Host "  后端日志:      backend.out.log / backend.err.log" -ForegroundColor DarkGray
Write-Host "  前端日志:      frontend.out.log / frontend.err.log" -ForegroundColor DarkGray
Write-Host '=============================================' -ForegroundColor Green

if ($Test) {
  Write-Host "`n[TEST] self-check passed. Stopping managed processes..." -ForegroundColor Cyan
  Stop-All
  Write-Host '[TEST] done.' -ForegroundColor Cyan
  exit 0
}

# 开发模式：前台阻塞，Ctrl+C 时清理
try {
  Write-Host "`nPress Ctrl+C to stop all services." -ForegroundColor Yellow
  while ($true) { Start-Sleep -Seconds 1 }
} finally {
  Stop-All
}
