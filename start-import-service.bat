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
gradle :import-service:bootRun
pause
