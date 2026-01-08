@echo off
echo ========================================
echo WMS Terminal - Environment Startup
echo ========================================
echo.

echo [1/3] Starting PostgreSQL...
start "PostgreSQL" cmd /k "docker compose up postgres"
timeout /t 5 /nobreak >nul

echo [2/3] Starting Core API (will take ~40 seconds)...
start "Core API" cmd /k "gradle :core-api:bootRun"
timeout /t 45 /nobreak >nul

echo [3/3] Starting Desktop Client...
start "Desktop Client" cmd /k "gradle :desktop-client:run"

echo.
echo ========================================
echo Environment started!
echo ========================================
echo.
echo PostgreSQL:     localhost:55432
echo Core API:       http://localhost:8080
echo Desktop Client: JavaFX window
echo.
echo Login credentials:
echo   Username: testuser
echo   Password: password
echo.
echo Press any key to close this window...
pause >nul
