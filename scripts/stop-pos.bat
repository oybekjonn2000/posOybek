@echo off
echo Stopping Restaurant POS services...

:: Kill processes on port 8080 (Spring Boot)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo Terminating backend process PID %%a...
    taskkill /F /PID %%a 2>nul
)

:: Kill processes on port 4200 (Angular dev server)
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":4200" ^| findstr "LISTENING"') do (
    echo Terminating frontend process PID %%a...
    taskkill /F /PID %%a 2>nul
)

echo All services stopped.
pause
