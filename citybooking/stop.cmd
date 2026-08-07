@echo off
setlocal

REM CityBooking 管理端停止脚本（双击入口）。透传参数给 stop.ps1。
REM 用法示例：
REM   stop.cmd           停止 前端(:5173) + 后端(:18100/:8080) + MySQL 容器
REM   stop.cmd -NoDB     不停止 MySQL 容器（保留数据库状态）
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0stop.ps1" %*

endlocal
