@echo off
echo ========================================
echo WMS Terminal - Environment Shutdown
echo ========================================
echo.

echo Stopping Docker containers...
docker compose down

echo.
echo Killing Java processes (Core API, Desktop Client)...
taskkill /F /FI "WINDOWTITLE eq Core API*" 2>nul
taskkill /F /FI "WINDOWTITLE eq Desktop Client*" 2>nul

echo.
echo ========================================
echo Environment stopped!
echo ========================================
echo.
pause
