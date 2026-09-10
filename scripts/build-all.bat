@echo off
echo ========================================================
echo        RESTAURANT POS - FULL PRODUCTION BUILD
echo ========================================================
echo.

set JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1
set PATH=C:\Users\Steam\tools\apache-maven-3.9.9\bin;%JAVA_HOME%\bin;%PATH%

:: 1. Build Angular
echo [1/2] Building Angular 22 Frontend...
cd /d %~dp0..\frontend\angular-pos
call ng build
if %errorlevel% neq 0 (
    echo [ERROR] Angular build failed!
    pause
    exit /b %errorlevel%
)

:: 2. Build Spring Boot JAR
echo.
echo [2/2] Building Spring Boot Fat JAR...
cd /d %~dp0..\backend\restaurant-pos-api
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Backend build failed!
    pause
    exit /b %errorlevel%
)

echo.
echo ========================================================
echo        BUILD SUCCESSFUL!
echo   - Backend JAR: backend\restaurant-pos-api\target\restaurant-pos-api-1.0.0-SNAPSHOT.jar
echo   - Frontend:    frontend\angular-pos\dist\angular-pos\browser\
echo ========================================================
pause
