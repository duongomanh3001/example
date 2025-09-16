@echo off
setlocal enabledelayedexpansion

:: CScore Jobe Server Management Script for Windows
:: Usage: jobe-manager.bat [command] [options]

set SCRIPT_DIR=%~dp0
set COMPOSE_FILE=%SCRIPT_DIR%docker-compose.jobe.yml
set JOBE_URL=http://localhost:4000

:: Check if Docker is available
where docker >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker is not installed or not in PATH
    exit /b 1
)

where docker-compose >nul 2>nul
if %ERRORLEVEL% neq 0 (
    docker compose version >nul 2>nul
    if !ERRORLEVEL! neq 0 (
        echo [ERROR] Docker Compose is not installed or not in PATH
        exit /b 1
    )
)

:: Main logic
if "%1"=="start" goto start_jobe
if "%1"=="stop" goto stop_jobe  
if "%1"=="restart" goto restart_jobe
if "%1"=="status" goto status_jobe
if "%1"=="test" goto test_jobe
if "%1"=="logs" goto logs_jobe
if "%1"=="update" goto update_jobe
if "%1"=="cleanup" goto cleanup_jobe
if "%1"=="help" goto usage
if "%1"=="-h" goto usage
if "%1"=="--help" goto usage
if "%1"=="" (
    echo [ERROR] No command specified
    goto usage
)

echo [ERROR] Unknown command: %1
goto usage

:start_jobe
echo [INFO] Starting Jobe server...
if "%2"=="multi" (
    echo [INFO] Starting multiple Jobe instances with load balancer...
    docker-compose -f "%COMPOSE_FILE%" --profile multi-instance up -d
) else (
    docker-compose -f "%COMPOSE_FILE%" up -d jobe-server
)
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Jobe server started successfully
    call :wait_for_jobe
) else (
    echo [ERROR] Failed to start Jobe server
)
goto end

:stop_jobe
echo [INFO] Stopping Jobe server...
docker-compose -f "%COMPOSE_FILE%" --profile multi-instance down
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Jobe server stopped
) else (
    echo [ERROR] Failed to stop Jobe server
)
goto end

:restart_jobe
echo [INFO] Restarting Jobe server...
call :stop_jobe
timeout /t 2 /nobreak >nul
call :start_jobe %2
goto end

:status_jobe
echo [INFO] Checking Jobe server status...
echo.
echo Container Status:
docker-compose -f "%COMPOSE_FILE%" ps
echo.
echo API Status:
curl -s "%JOBE_URL%/jobe/index.php/restapi/languages" >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Jobe API is responsive
    echo.
    echo Supported Languages:
    curl -s "%JOBE_URL%/jobe/index.php/restapi/languages"
) else (
    echo [ERROR] Jobe API is not responsive
)
goto end

:wait_for_jobe
echo [INFO] Waiting for Jobe server to be ready...
set max_attempts=30
set attempt=0

:wait_loop
curl -s "%JOBE_URL%/jobe/index.php/restapi/languages" >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo.
    echo [SUCCESS] Jobe server is ready!
    goto :eof
)

set /a attempt+=1
if !attempt! geq !max_attempts! (
    echo.
    echo [ERROR] Jobe server failed to start within 60 seconds
    goto :eof
)

echo|set /p="."
timeout /t 2 /nobreak >nul
goto wait_loop

:test_jobe
echo [INFO] Testing Jobe server with sample code...

echo.
echo Testing Java:
curl -s -X POST "%JOBE_URL%/jobe/index.php/restapi/runs" ^
    -H "Content-Type: application/json" ^
    -d "{\"language_id\": \"java\", \"sourcecode\": \"public class Main {\\n    public static void main(String[] args) {\\n        System.out.println(\\\"Hello from Java!\\\");\\n    }\\n}\"}"

echo.
echo.
echo Testing Python:
curl -s -X POST "%JOBE_URL%/jobe/index.php/restapi/runs" ^
    -H "Content-Type: application/json" ^
    -d "{\"language_id\": \"python3\", \"sourcecode\": \"print(\\\"Hello from Python!\\\")\"}"

echo.
echo.
echo Testing C++:
curl -s -X POST "%JOBE_URL%/jobe/index.php/restapi/runs" ^
    -H "Content-Type: application/json" ^
    -d "{\"language_id\": \"cpp\", \"sourcecode\": \"#include <iostream>\\nusing namespace std;\\nint main() {\\n    cout << \\\"Hello from C++!\\\" << endl;\\n    return 0;\\n}\"}"

goto end

:logs_jobe
echo [INFO] Showing Jobe server logs...
docker-compose -f "%COMPOSE_FILE%" logs -f jobe-server
goto end

:update_jobe
echo [INFO] Pulling latest Jobe image...
docker-compose -f "%COMPOSE_FILE%" pull
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Jobe image updated
) else (
    echo [ERROR] Failed to update Jobe image
)
goto end

:cleanup_jobe
echo [INFO] Cleaning up Jobe resources...
docker-compose -f "%COMPOSE_FILE%" --profile multi-instance down -v --remove-orphans
docker system prune -f
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Cleanup completed
) else (
    echo [ERROR] Cleanup failed
)
goto end

:usage
echo CScore Jobe Server Management Script for Windows
echo.
echo Usage: %0 [command] [options]
echo.
echo Commands:
echo   start [multi]     Start Jobe server (add 'multi' for multiple instances)
echo   stop              Stop Jobe server
echo   restart [multi]   Restart Jobe server
echo   status            Check Jobe server status
echo   test              Test Jobe server with sample code
echo   logs              View Jobe server logs
echo   update            Pull latest Jobe image
echo   cleanup           Clean up Jobe resources
echo   help              Show this help message
echo.
echo Examples:
echo   %0 start          # Start single Jobe instance
echo   %0 start multi    # Start multiple instances with load balancer
echo   %0 test           # Test Jobe server functionality
echo.
goto end

:end
endlocal
