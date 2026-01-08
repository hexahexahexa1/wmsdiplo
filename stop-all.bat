@echo off
title WMSDIPL - Stop All Services
echo ========================================
echo    WMSDIPL - Stopping All Services
echo ========================================
echo.

REM Stop Core API (port 8080)
echo [1/3] Stopping Core API (port 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo Found process: %%a
    taskkill /F /PID %%a >nul 2>&1
    if %errorlevel% equ 0 (
        echo Core API stopped ✓
    )
)
echo.

REM Stop Import Service (port 8090)
echo [2/3] Stopping Import Service (port 8090)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8090 ^| findstr LISTENING') do (
    echo Found process: %%a
    taskkill /F /PID %%a >nul 2>&1
    if %errorlevel% equ 0 (
        echo Import Service stopped ✓
    )
)
echo.

REM Stop Desktop Client (Java processes with "desktop-client" in command line)
echo [3/3] Stopping Desktop Client...
for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| findstr "PID:"') do (
    taskkill /F /PID %%a >nul 2>&1
)
echo Desktop Client stopped ✓
echo.

echo ========================================
echo    All services stopped!
echo ========================================
echo.
echo Note: PostgreSQL container is still running.
echo To stop PostgreSQL: docker compose down
echo.
pause
