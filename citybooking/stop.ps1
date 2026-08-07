<#
.SYNOPSIS
  CityBooking 管理端一键停止脚本（Windows / PowerShell 7+，兼容 Windows PowerShell 5.1）
  与 start-admin.ps1 对称：停止 前端 Vite(:5173) + 后端 Spring Boot(:18100/8080) + MySQL(Docker)。

.DESCRIPTION
  按端口定位并结束进程：
    - 前端 vite dev  (:5173)
    - 后端 jar       (:18100，兼容 :8080 旧端口)
    - MySQL 容器     mysql-cb（除非 -NoDB）
  不依赖启动脚本记录的 PID，因此也能清理“非本脚本启动”的残留进程。

.PARAMETER NoDB
  跳过 MySQL 容器的停止（当你希望保留数据库状态 / 使用外部 MySQL 时）。

.PARAMETER Force
  在结束进程时强制 -Force（默认已强制，保留以便语义清晰）。
#>
[CmdletBinding()]
param(
  [switch]$NoDB,
  [switch]$Force
)

$ErrorActionPreference = 'Stop'
$root      = $PSScriptRoot
$CONTAINER = 'mysql-cb'

$BE_PORTS = @(18100, 8080)
$FE_PORT  = 5173

function Write-Step($m) { Write-Host "`n==> $m" -ForegroundColor Cyan }

function Stop-By-Port($port) {
  $pids = (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue).OwningProcess |
    Where-Object { $_ -and $_ -ne $PID } | Select-Object -Unique
  if (-not $pids) {
    Write-Host "   port $port : no listening process"
    return
  }
  foreach ($p in $pids) {
    try {
      $proc = Get-Process -Id $p -ErrorAction SilentlyContinue
      $name = if ($proc) { $proc.Name } else { "(pid $p)" }
      Stop-Process -Id $p -Force -ErrorAction Stop
      Write-Host "   killed $name (pid $p) on port $port"
    } catch {
      Write-Warning "   could not kill pid $p on port $port : $_"
    }
  }
}

# ---------------- main ----------------
Write-Host 'CityBooking 停止脚本 (stop launcher)' -ForegroundColor Green

Write-Step "Stopping admin frontend (vite :$FE_PORT)"
Stop-By-Port $FE_PORT

Write-Step 'Stopping backend (Spring Boot)'
foreach ($p in $BE_PORTS) { Stop-By-Port $p }

if ($NoDB) {
  Write-Host "`n(--NoDB) skipping MySQL container stop." -ForegroundColor Yellow
} else {
  Write-Step "Stopping MySQL container [$CONTAINER]"
  if (Get-Command docker -ErrorAction SilentlyContinue) {
    try { docker info *> $null } catch { Write-Warning 'Docker daemon not running; skip container stop.'; $NoDB = $true }
  } else {
    Write-Warning 'docker not found; skip container stop.'
    $NoDB = $true
  }
  if (-not $NoDB) {
    $exists = docker ps -a --format '{{.Names}}' | Where-Object { $_ -eq $CONTAINER }
    if (-not $exists) {
      Write-Host "   container [$CONTAINER] does not exist; nothing to stop"
    } else {
      $running = docker ps --format '{{.Names}}' | Where-Object { $_ -eq $CONTAINER }
      if (-not $running) { Write-Host "   container [$CONTAINER] already stopped" }
      else {
        docker stop $CONTAINER | Out-Null
        if ($LASTEXITCODE -eq 0) { Write-Host "   container [$CONTAINER] stopped" }
        else { Write-Warning "   failed to stop container [$CONTAINER]" }
      }
    }
  }
}

Write-Host "`n=============================================" -ForegroundColor Green
Write-Host '  全部服务已停止 / All services stopped' -ForegroundColor Green
Write-Host '=============================================' -ForegroundColor Green
