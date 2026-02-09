@echo off
title WMSDIPL - Stop Environment
echo ========================================
echo WMSDIPL - Environment Shutdown
echo ========================================
echo.

echo [1/2] Stopping application services...
call stop-all.bat
echo.

echo [2/2] Stopping Docker containers...
docker compose down
echo.

echo ========================================
echo Environment stopped!
echo ========================================
echo.
pause
