@echo off
title WMSDIPL - Full Import Test
echo =========================================================
echo    WMSDIPL - Полный Тест Процесса Импорта
echo =========================================================
echo.

REM Проверка предварительных условий
echo [Шаг 1/8] Проверка сервисов...
echo.

REM Проверка PostgreSQL
echo Проверка PostgreSQL...
docker ps | findstr wmsdipl-postgres >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] PostgreSQL не запущен!
    echo Запустите: start-postgres.bat
    pause
    exit /b 1
)
echo ✓ PostgreSQL запущен

REM Проверка Core API
echo Проверка Core API (порт 8080)...
netstat -ano | findstr :8080 | findstr LISTENING >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Core API не запущен!
    echo Запустите: start-core-api.bat
    pause
    exit /b 1
)
echo ✓ Core API запущен

REM Проверка Import Service
echo Проверка Import Service (порт 8090)...
netstat -ano | findstr :8090 | findstr LISTENING >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Import Service не запущен!
    echo Запустите: start-import-service.bat
    pause
    exit /b 1
)
echo ✓ Import Service запущен
echo.

REM Проверка health endpoints
echo [Шаг 2/8] Проверка health endpoints...
curl -s http://localhost:8080/actuator/health | findstr "UP" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Core API health: UP
) else (
    echo [WARNING] Core API health check failed
)
echo.

REM Показать текущие накладные (ДО импорта)
echo [Шаг 3/8] Текущие накладные в системе (ДО импорта):
echo.
curl -s -u testuser:password http://localhost:8080/api/receipts 2>nul | find "docNo"
echo.
echo Нажмите любую клавишу для продолжения...
pause >nul
echo.

REM Показать тестовые файлы
echo [Шаг 4/8] Подготовленные тестовые файлы:
echo.
dir /B testinput\*.xml 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Нет XML файлов в testinput\
    echo Создайте тестовые файлы в testinput\
    pause
    exit /b 1
)
echo.
echo Файлы готовы к импорту.
echo.

REM Показать содержимое первого файла
echo [Шаг 5/8] Содержимое первого тестового файла:
echo.
type testinput\receipt-test-001.xml
echo.
echo.

REM Настройка Import Service на testinput
echo [Шаг 6/8] ВАЖНО: Настройка Import Service
echo.
echo Для этого теста нужно:
echo 1. Остановить Import Service (Ctrl+C в окне Import Service)
echo 2. Установить переменную окружения:
echo    set WMS_IMPORT_FOLDER=testinput
echo 3. Перезапустить Import Service:
echo    start-import-service.bat
echo.
echo Или скопировать файлы в папку input\:
echo    copy testinput\*.xml input\
echo.
choice /C 12 /M "Выберите: 1=Я уже настроил Import Service на testinput, 2=Скопировать в input"
if %errorlevel% equ 2 (
    echo.
    echo Копирование файлов в input\...
    if not exist input mkdir input
    copy testinput\*.xml input\
    echo ✓ Файлы скопированы в input\
    echo.
    echo [Шаг 7/8] Ожидание импорта...
    echo Import Service обработает файлы автоматически (интервал: 2 секунды)
    echo Следите за логами в окне Import Service...
    echo.
    echo Ожидание 10 секунд...
    timeout /t 10 /nobreak
) else (
    echo.
    echo [Шаг 7/8] Переместите файлы вручную:
    echo    copy testinput\*.xml testinput\
    echo.
    echo Нажмите любую клавишу после перемещения файлов...
    pause >nul
)
echo.

REM Проверка результата
echo [Шаг 8/8] Проверка результата импорта:
echo.
echo Накладные в системе (ПОСЛЕ импорта):
echo.
curl -s -u testuser:password http://localhost:8080/api/receipts | find "docNo"
echo.
echo.

REM Детальная информация по новым накладным
echo Детальная информация по импортированным накладным:
echo.
for /f "tokens=*" %%i in ('curl -s -u testuser:password http://localhost:8080/api/receipts ^| find "RCV-2026-TEST"') do (
    echo %%i
)
echo.

REM Проверка в базе данных
echo Проверка в базе данных:
echo.
docker exec wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "SELECT id, doc_no, message_id, status, supplier, created_at FROM receipts WHERE doc_no LIKE 'RCV-2026-TEST%%' ORDER BY created_at DESC;"
echo.

echo =========================================================
echo                   Тест завершен
echo =========================================================
echo.
echo Что проверить:
echo 1. Накладные RCV-2026-TEST-001 и RCV-2026-TEST-002 созданы
echo 2. messageId: TEST-MSG-2026-001 и TEST-MSG-2026-002
echo 3. Статус: DRAFT
echo 4. Строки накладных созданы (2 строки в каждой)
echo.
echo Проверить строки накладной:
echo   curl -u testuser:password http://localhost:8080/api/receipts/{ID}/lines
echo.
echo Проверить в Desktop Client:
echo   1. Открыть Desktop Client
echo   2. Перейти в "Поступления"
echo   3. Нажать "Обновить"
echo   4. Найти RCV-2026-TEST-001 и RCV-2026-TEST-002
echo   5. Двойной клик для просмотра строк
echo.
pause
