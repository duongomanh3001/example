@echo off
echo ========================================
echo   CScore with Jobe Server Startup
echo ========================================

:: Check if Docker is running
echo [STEP 1/5] Checking Docker...
docker version >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)
echo [SUCCESS] Docker is running

:: Start Jobe Server
echo [STEP 2/5] Starting Jobe Server...
call jobe-manager.bat start
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to start Jobe Server
    pause
    exit /b 1
)

:: Wait for Jobe to be ready
echo [STEP 3/5] Waiting for Jobe Server to be ready...
timeout /t 15 /nobreak >nul

:: Test Jobe Server
echo [STEP 4/5] Testing Jobe Server...
curl -s http://localhost:4000/jobe/index.php/restapi/languages >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [WARNING] Jobe Server might not be fully ready yet
    echo [INFO] You can test it later with: jobe-manager.bat test
) else (
    echo [SUCCESS] Jobe Server is ready
)

:: Start Backend
echo [STEP 5/5] Starting Backend and Frontend...
echo [INFO] Starting CScore Backend...
cd cscore-backend
start "CScore Backend" cmd /k "echo Starting Backend with Jobe integration... && mvnw spring-boot:run"

:: Wait a moment then start Frontend  
timeout /t 3 /nobreak >nul
cd ..
echo [INFO] Starting CScore Frontend...
start "CScore Frontend" cmd /k "echo Starting Frontend... && npm run dev"

echo.
echo ========================================
echo       ALL SERVICES STARTING...
echo ========================================
echo.
echo Services will be available at:
echo   Frontend:    http://localhost:3000
echo   Backend:     http://localhost:8086
echo   Jobe Server: http://localhost:4000
echo.
echo Wait 30-60 seconds for all services to fully start.
echo.
echo To check status:
echo   - Backend logs: Check 'CScore Backend' window
echo   - Frontend logs: Check 'CScore Frontend' window  
echo   - Jobe status: jobe-manager.bat status
echo.
echo To stop all services:
echo   - Close the Backend and Frontend windows
echo   - Run: jobe-manager.bat stop
echo.
pause
