@echo off
setlocal

REM CityBooking dev stopper (Windows cmd)
REM Stops the backend Java process and the Docker MySQL container used by start-dev.cmd.

set CONTAINER=mysql-cb

goto :main

REM ---------- centralized error exit (keep window open so the cause is visible) ----------
:die
echo.
echo [FAILED] %~nx0 stopped. Read the messages above.
echo Press any key to close this window...
pause >nul
exit /b 1

:main

REM ---------- 1. Stop backend Java process holding the app ports ----------
echo ==^> Stopping CityBooking backend (ports 8080 / 18100)
set FOUND=0
for %%p in (8080 18100) do (
    for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":%%p " ^| findstr "LISTENING"') do (
        echo ==^> Killing pid %%a on port %%p
        taskkill /pid %%a /f >nul 2>nul
        set FOUND=1
    )
)
if %FOUND%==0 (
    echo ==^> No backend process listening on 8080 / 18100.
) else (
    echo ==^> Backend stop signal sent.
)

REM ---------- 2. Stop Docker MySQL container ----------
where docker >nul 2>nul
if errorlevel 1 (
    echo docker not found. Skipping container stop.
    goto :done
)
docker info >nul 2>nul
if errorlevel 1 (
    echo Docker daemon is not running. Skipping container stop.
    goto :done
)

docker inspect -f "{{.Id}}" %CONTAINER% >nul 2>nul
if errorlevel 1 (
    echo ==^> Container %CONTAINER% does not exist. Nothing to stop.
    goto :done
)

docker ps --filter "status=running" --format "{{.Names}}" | findstr /x "%CONTAINER%" >nul
if errorlevel 1 (
    echo ==^> Container %CONTAINER% is not running.
) else (
    echo ==^> Stopping container %CONTAINER%
    docker stop %CONTAINER%
    if errorlevel 1 (
        echo Failed to stop container %CONTAINER%.
        goto :die
    )
    echo ==^> Container %CONTAINER% stopped.
)

:done
echo.
echo CityBooking dev environment stopped.
echo Press any key to close this window...
pause >nul
exit /b 0
