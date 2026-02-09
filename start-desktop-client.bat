@echo off
title Desktop Client
cd /d E:\WMSDIPL
echo Starting Desktop Client...
call gradlew.bat :shared-contracts:jar --no-daemon
if %errorlevel% neq 0 (
  echo Failed to build shared-contracts.
  pause
  exit /b 1
)
gradlew.bat :desktop-client:run -x :shared-contracts:jar --no-daemon
pause
