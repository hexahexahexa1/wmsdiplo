@echo off
title WMSDIPL - Start All Services
echo ========================================
echo    WMSDIPL - Starting All Services
echo ========================================
echo.
echo This will start:
echo  1. Core API (port 8080)
echo  2. Import Service (port 8090)
echo  3. Desktop Client
echo.
echo Press any key to continue or Ctrl+C to cancel...
pause >nul
echo.

REM Check if PostgreSQL is running
echo [1/4] Checking PostgreSQL...
docker ps | findstr wmsdipl-postgres >nul 2>&1
if %errorlevel% neq 0 (
    echo PostgreSQL is not running!
    echo Starting PostgreSQL...
    docker compose up -d postgres
    timeout /t 5 /nobreak >nul
) else (
    echo PostgreSQL is already running ✓
)
echo.

REM Start Core API in new window
echo [2/4] Starting Core API...
start "Core API (Port 8080)" cmd /k "cd /d E:\WMSDIPL && gradle :core-api:bootRun"
echo Waiting for Core API to start...
timeout /t 10 /nobreak >nul
echo.

REM Start Import Service in new window
echo [3/4] Starting Import Service...
start "Import Service (Port 8090)" cmd /k "cd /d E:\WMSDIPL && gradle :import-service:bootRun"
echo Waiting for Import Service to start...
timeout /t 5 /nobreak >nul
echo.

REM Start Desktop Client in new window
echo [4/4] Starting Desktop Client...
start "Desktop Client" cmd /k "cd /d E:\WMSDIPL && gradle :desktop-client:run"
echo.

echo ========================================
echo    All services are starting!
echo ========================================
echo.
echo Check the opened windows for logs:
echo  - Core API: http://localhost:8080
echo  - Import Service: http://localhost:8090
echo  - Desktop Client: JavaFX window
echo.
echo Press any key to exit this window...
pause >nul
