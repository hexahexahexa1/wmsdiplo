@echo off
title Import Service
cd /d E:\WMSDIPL
echo Starting Import Service...
echo.
echo Import Service will run on port 8090
echo Watching folder: %WMS_IMPORT_FOLDER%
echo Using credentials: testuser / password
echo.
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
