@echo off
setlocal
title WMSDIPL - Stop All Services
echo ========================================
echo    WMSDIPL - Stopping All Services
echo ========================================
echo.

echo [1/4] Stopping listeners on API ports...
for %%P in (8080 8090) do (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$port=%%P; $owning=Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique; foreach($procId in $owning){ try { Stop-Process -Id $procId -Force -ErrorAction Stop; Write-Output ('Stopped PID ' + $procId + ' on port ' + $port) } catch {} }"
)
echo.

echo [2/4] Stopping WMSDIPL Java/cmd processes...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$targets=Get-CimInstance Win32_Process | Where-Object { $null -ne $_.CommandLine -and $_.Name -match '^(java|javaw|cmd)\.exe$' -and ($_.CommandLine -like '*:core-api:bootRun*' -or $_.CommandLine -like '*:import-service:bootRun*' -or $_.CommandLine -like '*:desktop-client:run*' -or $_.CommandLine -like '*CoreApiApplication*' -or $_.CommandLine -like '*ImportServiceApplication*' -or $_.CommandLine -like '*DesktopClientApplication*') };" ^
    "foreach($t in $targets){ try { Stop-Process -Id $t.ProcessId -Force -ErrorAction Stop; Write-Output ('Stopped PID ' + $t.ProcessId + ' (' + $t.Name + ')') } catch {} }"
echo.

echo [3/4] Stopping Gradle daemons...
call gradlew.bat --stop >nul 2>&1
echo Gradle daemons stopped.
echo.

echo [4/4] Checking for remaining shared-contracts lock holders...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$left=Get-CimInstance Win32_Process | Where-Object { $null -ne $_.CommandLine -and $_.Name -match '^(java|javaw|cmd)\.exe$' -and $_.CommandLine -like '*shared-contracts-0.1.0-SNAPSHOT.jar*' }; if($left){ Write-Output 'Warning: potential lock holders still running:'; $left | Select-Object ProcessId,Name,CommandLine | Format-Table -AutoSize } else { Write-Output 'No shared-contracts lock holders found.' }"

echo ========================================
echo    All services stopped!
echo ========================================
echo.
echo Note: PostgreSQL container is still running.
echo To stop PostgreSQL: docker compose down
echo.
pause
