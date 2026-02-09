@echo off
title Core API Server
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

cd /d E:\WMSDIPL
echo Starting Core API with minimal memory settings...
echo JAVA_HOME: %JAVA_HOME%
echo.

call gradlew.bat :shared-contracts:jar --no-daemon
if %errorlevel% neq 0 (
  echo Failed to build shared-contracts.
  pause
  exit /b 1
)

gradlew.bat :core-api:bootRun -x :shared-contracts:jar --no-daemon -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m"
pause
