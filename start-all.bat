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
echo [1/5] Checking PostgreSQL...
docker ps | findstr wmsdipl-postgres >nul 2>&1
if %errorlevel% neq 0 (
    echo PostgreSQL is not running!
    echo Starting PostgreSQL...
    docker compose up -d postgres
    echo Waiting for PostgreSQL to be ready...
    timeout /t 10 /nobreak >nul
) else (
    echo PostgreSQL is already running ✓
)
echo.

REM Check if database is initialized
echo [2/5] Checking database...
docker exec wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "SELECT 1 FROM zones LIMIT 1;" >nul 2>&1
if %errorlevel% neq 0 (
    echo Database not initialized or empty!
    echo Restoring database schema...
    docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "DROP DATABASE IF EXISTS wmsdipl;" >nul 2>&1
    docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "CREATE DATABASE wmsdipl;" >nul 2>&1
    docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl < database\init_schema.sql >nul 2>&1
    echo Database schema restored ✓
) else (
    echo Database is ready ✓
)
echo.

REM Start Core API in new window
echo [3/5] Starting Core API...
start "Core API (Port 8080)" cmd /k "cd /d E:\WMSDIPL && set JAVA_OPTS=-Xmx512m -XX:MaxMetaspaceSize=256m && gradle :core-api:bootRun"
echo Waiting for Core API to start...
timeout /t 15 /nobreak >nul
echo.

REM Start Import Service in new window
echo [4/5] Starting Import Service...
start "Import Service (Port 8090)" cmd /k "cd /d E:\WMSDIPL && set JAVA_OPTS=-Xmx256m -XX:MaxMetaspaceSize=128m && gradle :import-service:bootRun"
echo Waiting for Import Service to start...
timeout /t 10 /nobreak >nul
echo.

REM Start Desktop Client in new window
echo [5/5] Starting Desktop Client...
start "Desktop Client" cmd /k "cd /d E:\WMSDIPL && set JAVA_OPTS=-Xmx256m -XX:MaxMetaspaceSize=128m && gradle :desktop-client:run"
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
