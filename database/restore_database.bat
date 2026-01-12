@echo off
chcp 65001 >nul
echo ============================================
echo   Восстановление базы данных WMSDIPL
echo ============================================
echo.

docker ps | findstr wmsdipl-postgres >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] PostgreSQL контейнер не запущен!
    echo          Запустите: docker compose up -d postgres
    pause
    exit /b 1
)
echo [OK] PostgreSQL контейнер запущен

echo.
echo [1/4] Ожидание готовности PostgreSQL...
:wait_loop
docker exec wmsdipl-postgres pg_isready -U wmsdipl >nul 2>&1
if errorlevel 1 (
    timeout /t 1 /nobreak >nul
    goto wait_loop
)
echo [OK] PostgreSQL готов

echo.
echo [2/4] Удаление старой базы данных...
docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "DROP DATABASE IF EXISTS wmsdipl;" >nul 2>&1
echo [OK] Старая база удалена

echo.
echo [3/4] Создание новой базы данных...
docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "CREATE DATABASE wmsdipl;" >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Не удалось создать базу данных
    pause
    exit /b 1
)
echo [OK] База данных создана

echo.
echo [4/4] Восстановление схемы из init_schema.sql...
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl < database\init_schema.sql >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Не удалось восстановить схему
    pause
    exit /b 1
)
echo [OK] Схема восстановлена

echo.
echo ============================================
echo   База данных успешно восстановлена!
echo ============================================
echo.
echo Список таблиц:
docker exec wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "\dt"

echo.
pause
