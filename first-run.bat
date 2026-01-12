@echo off
chcp 65001 >nul
title WMSDIPL - Первый запуск
echo ============================================
echo   WMSDIPL - Первый запуск / Инициализация
echo ============================================
echo.
echo Этот скрипт выполнит:
echo  1. Запуск PostgreSQL
echo  2. Инициализацию базы данных
echo  3. Проверку готовности
echo.
echo Нажмите любую клавишу для продолжения...
pause >nul
echo.

echo [1/3] Запуск PostgreSQL...
docker compose up -d postgres
if errorlevel 1 (
    echo [ОШИБКА] Не удалось запустить PostgreSQL
    echo Проверьте, что Docker запущен
    pause
    exit /b 1
)
echo [OK] PostgreSQL запущен

echo.
echo [2/3] Ожидание готовности PostgreSQL (до 30 сек)...
set /a counter=0
:wait_loop
docker exec wmsdipl-postgres pg_isready -U wmsdipl >nul 2>&1
if errorlevel 0 goto db_ready
set /a counter+=1
if %counter% GEQ 30 (
    echo [ОШИБКА] PostgreSQL не готов после 30 секунд
    pause
    exit /b 1
)
timeout /t 1 /nobreak >nul
goto wait_loop

:db_ready
echo [OK] PostgreSQL готов

echo.
echo [3/3] Инициализация базы данных...
call database\restore_database.bat
if errorlevel 1 (
    echo [ОШИБКА] Не удалось инициализировать базу данных
    pause
    exit /b 1
)

echo.
echo ============================================
echo   Инициализация завершена успешно!
echo ============================================
echo.
echo Теперь вы можете запустить приложения:
echo   start-all.bat        - Запустить все сервисы
echo   start-core-api.bat   - Только Core API
echo   start-desktop-client.bat - Только Desktop Client
echo.
pause
