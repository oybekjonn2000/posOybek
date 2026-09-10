@echo off
echo ========================================================
echo        RESTAURANT POS - SYSTEM STARTUP
echo ========================================================
echo.

:: 1. Check PostgreSQL
echo [1/3] Checking PostgreSQL service...
net start | findstr /i "postgresql-x64-18" > nul
if %errorlevel% neq 0 (
    echo Starting PostgreSQL service...
    net start postgresql-x64-18
) else (
    echo PostgreSQL service is already running.
)
echo.

:: 2. Start Spring Boot API
echo [2/3] Starting Spring Boot API (port 8080)...
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1
set PATH=%JAVA_HOME%\bin;%PATH%

start "Restaurant POS API" cmd /k "cd /d %~dp0..\backend\restaurant-pos-api && java -jar target\restaurant-pos-api-1.0.0-SNAPSHOT.jar"
echo Backend launched in background window.
echo.

:: 3. Start Angular Web UI
echo [3/3] Starting Frontend Client (port 4200)...
start "Restaurant POS UI" cmd /k "cd /d %~dp0..\frontend\angular-pos && npm start"
echo Frontend launched in background window.
echo.

echo ========================================================
echo   System will be available at:
echo   - Web POS:    http://localhost:4200
echo   - Backend API: http://localhost:8080/swagger-ui.html
echo   - Health:     http://localhost:8080/actuator/health
echo ========================================================
pause
