# start-dev.ps1 -- one-click local verification launcher
# Starts the backend (H2 dev profile, no external DB), runs the mini-program
# gate, smoke-tests the live API, and (if installed) opens the mini-program
# in WeChat DevTools. ASCII-only to avoid encoding issues.
$ErrorActionPreference = 'Continue'
$ROOT = Split-Path -Parent $MyInvocation.MyCommand.Definition
$BASE = 'http://127.0.0.1:8080/api'
$SERVER_JAR = Join-Path $ROOT 'server\target\server-1.0.0.jar'
$MINI = Join-Path $ROOT 'miniprogram'

function Step($m) { Write-Host ("[STEP] " + $m) -ForegroundColor Cyan }
function Ok($m)   { Write-Host ("  OK  " + $m) -ForegroundColor Green }
function Bad($m)  { Write-Host ("  FAIL " + $m) -ForegroundColor Red }

function Curl-Json($uri, $method, $body, $token) {
    $h = @{'Content-Type' = 'application/json'}
    if ($token) { $h['Authorization'] = ('Bearer ' + $token) }
    if ($body) {
        return Invoke-RestMethod -Uri $uri -Method $method -Headers $h -Body $body
    }
    return Invoke-RestMethod -Uri $uri -Method $method -Headers $h
}

# ---- 1) ensure backend is up ----
Step "Ensure backend is running at $BASE"
$already = $false
try { $null = Curl-Json ($BASE + '/auth/me') Get $null $null; $already = $true } catch { }

if (-not $already) {
    if (-not (Test-Path $SERVER_JAR)) {
        Write-Host "  JAR not found, building with mvn..." -ForegroundColor Yellow
        Push-Location (Join-Path $ROOT 'server')
        & mvn -q package -DskipTests
        Pop-Location
    }
    if (-not (Test-Path $SERVER_JAR)) { Bad "server jar missing after build"; exit 1 }

    $log = Join-Path $ROOT 'server-dev.log'
    $err = Join-Path $ROOT 'server-dev.err'
    Write-Host "  launching java -jar (dev profile, H2 in-memory)..." -ForegroundColor Yellow
    Start-Process -FilePath 'java' `
        -ArgumentList "-jar","`"$SERVER_JAR`"","--spring.profiles.active=dev" `
        -RedirectStandardOutput $log -RedirectStandardError $err -WindowStyle Hidden

    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 2
        # readiness = server answers ANY HTTP response on the auth endpoint
        # (security may block /actuator/health with 403, so we do not rely on it)
        try {
            $null = Invoke-WebRequest -Uri ($BASE + '/auth/login') -Method Post `
                -ContentType 'application/json' -Body '{}' -UseBasicParsing `
                -TimeoutSec 3 -ErrorAction SilentlyContinue
            $ready = $true; break
        } catch {
            if ($_.Exception.Response) { $ready = $true; break }
        }
    }
    if (-not $ready) { Bad "backend not ready in 120s. Tail server-dev.log:"; Get-Content $log -Tail 20; exit 1 }
    Ok "backend ready"
} else {
    Ok "backend already running"
}

# ---- 2) mini-program gate ----
Step "Run mini-program gate (node miniprogram/scripts/gate.js)"
Push-Location $MINI
& node scripts/gate.js
$gateRc = $LASTEXITCODE
Pop-Location
if ($gateRc -eq 0) { Ok "mini-program gate PASS" } else { Bad "mini-program gate FAIL (rc=$gateRc)" }

# ---- 3) live API smoke test ----
Step "Smoke test live API: register -> login -> wechat-login -> /auth/me"
try {
    $phone = '138' + (Get-Random -Minimum 10000000 -Maximum 99999999)
    $reg = Curl-Json ($BASE + '/auth/register') Post (ConvertTo-Json @{phone=$phone; password='Test1234'; role='CONSUMER'; nickname='smoke'})
    Ok ("register userId=" + $reg.data.userId)

    $login = Curl-Json ($BASE + '/auth/login') Post (ConvertTo-Json @{phone=$phone; password='Test1234'})
    Ok ("login role=" + $login.data.role)

    $code = ('smoke_' + $phone)
    $wx = Curl-Json ($BASE + '/auth/wechat-login') Post (ConvertTo-Json @{code=$code})
    Ok ("wechat-login userId=" + $wx.data.userId)

    $me = Curl-Json ($BASE + '/auth/me') Get $null $wx.data.token
    Ok ("/auth/me role=" + $me.data.role)
} catch {
    Bad ("smoke test error: " + $_.Exception.Message)
}

# ---- 4) WeChat DevTools (optional) ----
Step "Locate / open WeChat DevTools"
$cli = $null
$cands = @(
    'C:\Program Files (x86)\Tencent\微信web开发者工具\cli.bat',
    'C:\Program Files\Tencent\微信web开发者工具\cli.bat',
    'C:\Program Files (x86)\Tencent\微信web开发者工具\cli.exe',
    'C:\Program Files\Tencent\微信web开发者工具\cli.exe'
)
foreach ($c in $cands) { if (Test-Path $c) { $cli = $c; break } }
if (-not $cli) {
    $f = Get-ChildItem -Path 'C:\Program Files*' -Recurse -Filter 'cli.bat' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($f) { $cli = $f.FullName }
}
if ($cli) {
    Ok ("found DevTools CLI: " + $cli)
    try { & $cli open --project "$MINI" | Out-Null; Ok "opened mini-program project in DevTools" }
    catch { Bad ("failed to open project: " + $_) }
} else {
    Write-Host "  WeChat DevTools not detected. To verify the mini-program UI:" -ForegroundColor Yellow
    Write-Host "    1) Install WeChat DevTools: https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html" -ForegroundColor Yellow
    Write-Host "    2) Open it, choose 'Import Project', select folder:" -ForegroundColor Yellow
    Write-Host ("       $MINI") -ForegroundColor Yellow
    Write-Host "    3) AppID: use 'touristappid' (test number). Backend is already at $BASE." -ForegroundColor Yellow
}

# ---- summary ----
Write-Host ""
Write-Host "================ VERIFY SUMMARY ================" -ForegroundColor Magenta
Write-Host ("Backend API : $BASE  (running in background)") -ForegroundColor White
Write-Host ("Mini-program: $MINI") -ForegroundColor White
Write-Host ("H2 console : http://127.0.0.1:8080/api/h2-console") -ForegroundColor White
Write-Host "Next: open the mini-program in WeChat DevTools and try login / wechat-login / browse." -ForegroundColor White
Write-Host "Stop backend later with:  Stop-Process -Name java  (or close its window)" -ForegroundColor Gray
Write-Host "================================================" -ForegroundColor Magenta
