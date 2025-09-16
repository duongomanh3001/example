@echo off
echo ========================================
echo     CScore Services Shutdown
echo ========================================

echo [STEP 1/4] Stopping Jobe Server...
call jobe-manager.bat stop
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Jobe Server stopped
) else (
    echo [WARNING] Could not stop Jobe Server or it was not running
)

echo [STEP 2/4] Stopping Backend...
echo [INFO] Please manually close the 'CScore Backend' command window if still open
taskkill /f /im java.exe >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Java processes stopped
)

echo [STEP 3/4] Stopping Frontend...  
echo [INFO] Please manually close the 'CScore Frontend' command window if still open
taskkill /f /im node.exe >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Node.js processes stopped
)

echo [STEP 4/4] Stopping Docker (optional)...
echo [INFO] Choose Docker shutdown option:
echo   1. Keep Docker running (recommended)
echo   2. Stop Docker containers only
echo   3. Stop Docker Desktop completely
echo.
set /p choice="Enter choice (1-3) or press Enter for option 1: "

if "%choice%"=="" set choice=1
if "%choice%"=="1" (
    echo [INFO] Docker Desktop will remain running
    goto cleanup
)

if "%choice%"=="2" (
    echo [INFO] Stopping Docker containers...
    docker ps -q > temp_containers.txt 2>nul
    if exist temp_containers.txt (
        for /f %%i in (temp_containers.txt) do docker stop %%i >nul 2>nul
        del temp_containers.txt
        echo [SUCCESS] Docker containers stopped
    ) else (
        echo [INFO] No running containers found
    )
    goto cleanup
)

if "%choice%"=="3" (
    echo [INFO] Stopping Docker Desktop completely...
    
    REM Stop Docker containers first
    docker ps -q > temp_containers.txt 2>nul
    if exist temp_containers.txt (
        for /f %%i in (temp_containers.txt) do docker stop %%i >nul 2>nul
        del temp_containers.txt
        echo [SUCCESS] Docker containers stopped
    )
    
    REM Stop Docker Desktop
    taskkill /f /im "Docker Desktop.exe" >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        echo [SUCCESS] Docker Desktop stopped
    )
    
    REM Stop Docker services
    net stop com.docker.service >nul 2>nul
    net stop docker >nul 2>nul
    echo [SUCCESS] Docker services stopped
)

:cleanup

echo.
echo ========================================
echo        CLEANUP COMPLETED
echo ========================================
echo.
echo All CScore services have been stopped.
echo.
pause
