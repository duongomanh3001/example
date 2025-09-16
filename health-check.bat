@echo off
echo ========================================
echo    CScore System Health Check
echo ========================================

:: Check Docker
echo [1/6] Checking Docker...
docker --version >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker not found
) else (
    echo [SUCCESS] Docker is available
)

:: Check Java
echo [2/6] Checking Java...
java -version >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java not found
) else (
    echo [SUCCESS] Java is available
    java -version 2>&1 | findstr "version"
)

:: Check Node.js
echo [3/6] Checking Node.js...
node --version >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Node.js not found
) else (
    echo [SUCCESS] Node.js is available
    node --version
)

:: Check MySQL Connection
echo [4/6] Checking MySQL Connection...
mysql --version >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [WARNING] MySQL client not found in PATH
) else (
    echo [SUCCESS] MySQL client is available
    mysql --version
)

:: Check Jobe Server
echo [5/6] Checking Jobe Server...
docker ps | findstr jobe >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [INFO] Jobe Server container not running
    echo        Run: jobe-manager.bat start
) else (
    echo [SUCCESS] Jobe Server container is running
    curl -s http://localhost:4000/jobe/index.php/restapi/languages >nul 2>nul
    if %ERRORLEVEL% neq 0 (
        echo [WARNING] Jobe API not responding
    ) else (
        echo [SUCCESS] Jobe API is responding
    )
)

:: Check Backend
echo [6/6] Checking Backend...
curl -s http://localhost:8086/actuator/health >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [INFO] Backend not responding on port 8086
    echo        Backend may not be started yet
) else (
    echo [SUCCESS] Backend is responding
)

echo.
echo ========================================
echo         HEALTH CHECK SUMMARY
echo ========================================

:: Show running services
echo Current running services:
netstat -an | findstr ":3000.*LISTENING" >nul 2>nul && echo   - Frontend: http://localhost:3000
netstat -an | findstr ":8086.*LISTENING" >nul 2>nul && echo   - Backend: http://localhost:8086  
netstat -an | findstr ":4000.*LISTENING" >nul 2>nul && echo   - Jobe Server: http://localhost:4000
netstat -an | findstr ":3306.*LISTENING" >nul 2>nul && echo   - MySQL: localhost:3306

echo.
echo To start all services: start-all.bat
echo To stop all services: stop-all.bat
echo.
pause
