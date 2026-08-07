@echo off
setlocal

REM CityBooking dev launcher (Windows cmd)
REM Starts/reuses Docker MySQL(8), waits until ready, then builds and runs the backend jar.

set CONTAINER=mysql-cb
set DB_PORT=3306
set MYSQL_ROOT_PASSWORD=root
set DB_NAME=citybooking_dev
set MYSQL_IMAGE=mysql:8
set SERVER_DIR=%~dp0server

goto :main

REM ---------- centralized error exit (keep window open so the cause is visible) ----------
:die
echo.
echo [FAILED] %~nx0 stopped. Read the messages above.
echo Press any key to close this window...
pause >nul
exit /b 1

:main

REM ---------- 1. Check docker ----------
where docker >nul 2>nul
if errorlevel 1 (
    echo docker not found. Install Docker Desktop and start it first.
    goto :die
)
docker info >nul 2>nul
if errorlevel 1 (
    echo Docker daemon is not running. Start Docker Desktop first.
    goto :die
)

REM ---------- 2. Create / start MySQL container ----------
docker inspect -f "{{.Id}}" %CONTAINER% >nul 2>nul
if errorlevel 1 (
    echo ==^> Creating and starting MySQL container %CONTAINER%
    docker run -d --name %CONTAINER% -e "MYSQL_ROOT_PASSWORD=%MYSQL_ROOT_PASSWORD%" -e "MYSQL_DATABASE=%DB_NAME%" -p %DB_PORT%:3306 %MYSQL_IMAGE%
    if errorlevel 1 goto :die
) else (
    docker ps --filter "status=running" --format "{{.Names}}" | findstr /x "%CONTAINER%" >nul
    if errorlevel 1 (
        echo ==^> Starting existing container %CONTAINER%
        docker start %CONTAINER%
        if errorlevel 1 goto :die
    ) else (
        echo ==^> MySQL container %CONTAINER% is already running.
    )
)

REM ---------- 3. Wait for MySQL ----------
echo ==^> Waiting for MySQL to be ready, up to 60 seconds
set READY=0
for /l %%i in (1,1,30) do (
    docker exec %CONTAINER% mysqladmin ping -h 127.0.0.1 -uroot -p%MYSQL_ROOT_PASSWORD% 2>nul | findstr "alive" >nul
    if not errorlevel 1 (
        set READY=1
        goto :mysql_ready
    )
    timeout /t 2 >nul
)
:mysql_ready
if %READY%==0 (
    echo MySQL was not ready in time. Check logs: docker logs %CONTAINER%
    goto :die
)
echo ==^> MySQL is ready on localhost:%DB_PORT% db %DB_NAME% user root

REM ---------- 4. Pick Maven command ----------
where mvn >nul 2>nul
if not errorlevel 1 (
    set MVN=mvn
) else (
    if exist "%SERVER_DIR%\mvnw.cmd" (
        set MVN=%SERVER_DIR%\mvnw.cmd
    ) else (
        echo Neither mvn nor mvnw found. Install Maven or keep mvnw in the project.
        goto :die
    )
)

REM ---------- 5. Free app ports (stop leftover backend holding target jar / port) ----------
for %%p in (8080 18100) do (
    for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":%%p " ^| findstr "LISTENING"') do (
        echo ==^> Freeing port %%p, pid %%a
        taskkill /pid %%a /f >nul 2>nul
    )
)
timeout /t 2 >nul

REM ---------- 6. Build & launch backend ----------
cd /d %SERVER_DIR%
call %MVN% clean package -DskipTests
if errorlevel 1 (
    echo Backend build failed. See Maven output above.
    goto :die
)
for /f "delims=" %%j in ('dir /b target\server-*.jar 2^>nul ^| findstr /v "\.original"') do set JAR=%%j
if not defined JAR (
    echo Built jar not found in target\. Build may have produced no artifact.
    goto :die
)
echo ==^> Launching backend jar, dev profile uses MySQL: %JAR%
java -jar target\%JAR%

REM backend process ended (stopped or crashed) - keep window open to inspect logs
echo.
echo Backend process ended. Press any key to close this window...
pause >nul
