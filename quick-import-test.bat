@echo off
title Quick Import Test
echo ========================================
echo   Быстрый Тест Импорта
echo ========================================
echo.

REM Проверка сервисов
echo [1/4] Проверка сервисов...
netstat -ano | findstr :8080 | findstr LISTENING >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Core API не запущен! Запустите: start-core-api.bat
    pause
    exit /b 1
)

netstat -ano | findstr :8090 | findstr LISTENING >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Import Service не запущен! Запустите: start-import-service.bat
    pause
    exit /b 1
)
echo ✓ Все сервисы запущены
echo.

REM Создать папку input если не существует
if not exist input mkdir input

REM Копировать тестовые файлы в input
echo [2/4] Копирование тестовых файлов в input\...
copy /Y testinput\*.xml input\
echo ✓ Файлы скопированы
echo.

REM Ждать обработки
echo [3/4] Ожидание обработки Import Service (10 секунд)...
echo Следите за логами в окне Import Service!
timeout /t 10 /nobreak
echo.

REM Проверить результат
echo [4/4] Проверка результата:
echo.
curl -s -u testuser:password "http://localhost:8080/api/receipts" | find "RCV-2026-TEST"
echo.
echo.

echo ========================================
echo Импорт завершен!
echo.
echo Проверьте в Desktop Client:
echo   Поступления -^> Обновить -^> Найти RCV-2026-TEST-001 и RCV-2026-TEST-002
echo.
pause
