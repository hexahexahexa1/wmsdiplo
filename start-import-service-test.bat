@echo off
title Import Service - Test Mode
cd /d E:\WMSDIPL
echo Starting Import Service in TEST MODE...
echo.
echo Configuration:
echo - Port: 8090
echo - Folder: testinput
echo - Username: testuser
echo - Password: password
echo.
set WMS_IMPORT_FOLDER=testinput
set WMS_CORE_API_USERNAME=testuser
set WMS_CORE_API_PASSWORD=password
call gradlew.bat :shared-contracts:jar --no-daemon
if %errorlevel% neq 0 (
  echo Failed to build shared-contracts.
  pause
  exit /b 1
)
gradlew.bat :import-service:bootRun -x :shared-contracts:jar --no-daemon
pause
