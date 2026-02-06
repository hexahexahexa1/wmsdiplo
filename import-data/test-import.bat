@echo off
chcp 65001 > nul
REM Скрипт для быстрого тестирования импорта приходов (Windows)

setlocal

set EXAMPLES_DIR=examples
set INCOMING_DIR=incoming

echo === Тестирование импорта XML документов ===
echo.

REM Проверка директорий
if not exist "%EXAMPLES_DIR%" (
    echo ❌ Директория %EXAMPLES_DIR% не найдена!
    exit /b 1
)

if not exist "%INCOMING_DIR%" (
    echo 📁 Создание директории %INCOMING_DIR%...
    mkdir "%INCOMING_DIR%"
)

REM Меню выбора
echo Выберите тестовый файл для импорта:
echo.
echo 1^) receipt-simple.xml     - Простой пример ^(3 позиции^)
echo 2^) receipt-large.xml      - Большой заказ ^(12 позиций^)
echo 3^) receipt-minimal.xml    - Минимальный пример ^(2 позиции^)
echo 4^) receipt-weighted.xml   - Весовые товары ^(5 позиций^)
echo 5^) receipt-pharma.xml     - Фармацевтика ^(5 позиций^)
echo 6^) Все файлы сразу
echo 0^) Выход
echo.

set /p choice="Ваш выбор: "

if "%choice%"=="1" (
    call :import_file "%EXAMPLES_DIR%\receipt-simple.xml"
) else if "%choice%"=="2" (
    call :import_file "%EXAMPLES_DIR%\receipt-large.xml"
) else if "%choice%"=="3" (
    call :import_file "%EXAMPLES_DIR%\receipt-minimal.xml"
) else if "%choice%"=="4" (
    call :import_file "%EXAMPLES_DIR%\receipt-weighted.xml"
) else if "%choice%"=="5" (
    call :import_file "%EXAMPLES_DIR%\receipt-pharma.xml"
) else if "%choice%"=="6" (
    echo 📦 Импорт всех файлов...
    echo.
    for %%f in (%EXAMPLES_DIR%\*.xml) do (
        call :import_file "%%f"
    )
) else if "%choice%"=="0" (
    echo Выход.
    exit /b 0
) else (
    echo ❌ Неверный выбор!
    exit /b 1
)

echo.
echo === Готово! ===
echo.
echo Проверьте результат:
echo   • Desktop Client → Приходы
echo   • Логи import-service
echo   • Директория processed\ ^(успешные^)
echo   • Директория failed\ ^(ошибки^)
echo.
pause
exit /b 0

:import_file
set file=%~1
set filename=%~nx1

echo 📦 Импорт: %filename%
copy "%file%" "%INCOMING_DIR%\" > nul

echo    ✅ Файл скопирован в %INCOMING_DIR%\
echo    ⏳ Ожидайте обработки import-service ^(до 10 секунд^)...
echo.
exit /b 0
