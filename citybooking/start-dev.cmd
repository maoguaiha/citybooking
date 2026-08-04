@echo off
rem One-click local verification launcher for 同城预约服务平台.
rem Double-click this file (or run from a terminal) to start the backend,
rem run the mini-program gate, smoke-test the live API, and (if installed)
rem open the mini-program in WeChat DevTools.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-dev.ps1"
pause
