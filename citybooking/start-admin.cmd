@echo off
setlocal
REM 管理端启动脚本（双击入口）。透传参数给 start-admin.ps1。
REM 用法示例：
REM   start-admin.cmd           启动全部（后端会重新构建）
REM   start-admin.cmd -NoBuild  跳过后端构建，快速重启
REM   start-admin.cmd -Test     自检模式：启动->就绪->自动停止
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0start-admin.ps1" %*
endlocal
