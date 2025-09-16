@echo off
echo ========================================
echo      Docker Management Script
echo ========================================

if "%1"=="stop" goto stop_docker
if "%1"=="start" goto start_docker
if "%1"=="restart" goto restart_docker
if "%1"=="status" goto status_docker
if "%1"=="containers" goto list_containers
if "%1"=="cleanup" goto cleanup_docker

echo Usage: docker-manager.bat [command]
echo.
echo Commands:
echo   start      - Start Docker Desktop
echo   stop       - Stop Docker Desktop and containers
echo   restart    - Restart Docker Desktop
echo   status     - Check Docker status
echo   containers - List running containers
echo   cleanup    - Stop all containers and clean up
echo.
goto end

:start_docker
echo [INFO] Starting Docker Desktop...
start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
echo [INFO] Docker Desktop is starting... (may take 1-2 minutes)
echo [INFO] You can check status with: docker-manager.bat status
goto end

:stop_docker
echo [INFO] Stopping Docker completely...

echo [1/4] Stopping all running containers...
docker ps -q > temp_containers.txt 2>nul
if exist temp_containers.txt (
    for /f %%i in (temp_containers.txt) do (
        echo   Stopping container %%i...
        docker stop %%i >nul 2>nul
    )
    del temp_containers.txt
    echo [SUCCESS] All containers stopped
) else (
    echo [INFO] No running containers found
)

echo [2/4] Stopping Docker Desktop application...
taskkill /f /im "Docker Desktop.exe" >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Docker Desktop stopped
) else (
    echo [INFO] Docker Desktop was not running
)

echo [3/4] Stopping Docker services...
net stop com.docker.service >nul 2>nul
net stop docker >nul 2>nul
echo [SUCCESS] Docker services stopped

echo [4/4] Cleanup completed
goto end

:restart_docker
echo [INFO] Restarting Docker...
call :stop_docker
timeout /t 3 /nobreak >nul
call :start_docker
goto end

:status_docker
echo [INFO] Checking Docker status...
echo.

REM Check Docker Desktop process
tasklist /fi "imagename eq Docker Desktop.exe" 2>nul | find /i "Docker Desktop.exe" >nul
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Docker Desktop is running
) else (
    echo [INFO] Docker Desktop is not running
)

REM Check Docker daemon
docker version >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Docker daemon is responding
    docker version --format "Docker version: {{.Server.Version}}"
) else (
    echo [WARNING] Docker daemon is not responding
)

REM Check running containers
echo.
echo Running containers:
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" 2>nul
if %ERRORLEVEL% neq 0 (
    echo [INFO] No containers found or Docker not available
)

goto end

:list_containers
echo [INFO] Docker containers:
echo.
echo === RUNNING CONTAINERS ===
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
echo.
echo === ALL CONTAINERS ===
docker ps -a --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.CreatedAt}}"
goto end

:cleanup_docker
echo [INFO] Docker cleanup...

echo [1/3] Stopping all containers...
docker ps -q | ForEach-Object { docker stop $_ } >nul 2>nul

echo [2/3] Removing stopped containers...
docker container prune -f >nul 2>nul

echo [3/3] Removing unused images and networks...
docker system prune -f >nul 2>nul

echo [SUCCESS] Docker cleanup completed
goto end

:end
