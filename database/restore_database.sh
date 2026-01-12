#!/bin/bash
# restore_database.sh
# Скрипт для восстановления базы данных WMSDIPL из SQL дампа

echo "🔄 Восстановление базы данных WMSDIPL..."

# Проверяем, что PostgreSQL контейнер запущен
if ! docker ps | grep -q wmsdipl-postgres; then
    echo "❌ PostgreSQL контейнер не запущен!"
    echo "   Запустите: docker compose up -d postgres"
    exit 1
fi

echo "✓ PostgreSQL контейнер запущен"

# Ждем, пока PostgreSQL будет готов
echo "⏳ Ожидание готовности PostgreSQL..."
until docker exec wmsdipl-postgres pg_isready -U wmsdipl >/dev/null 2>&1; do
    sleep 1
done
echo "✓ PostgreSQL готов"

# Удаляем и пересоздаем базу данных
echo "🗑️  Удаление старой базы данных..."
docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "DROP DATABASE IF EXISTS wmsdipl;" 2>/dev/null

echo "📦 Создание новой базы данных..."
docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "CREATE DATABASE wmsdipl;"

# Восстанавливаем схему
echo "📊 Восстановление схемы из init_schema.sql..."
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl < database/init_schema.sql

if [ $? -eq 0 ]; then
    echo "✅ База данных успешно восстановлена!"
    echo ""
    echo "📋 Информация:"
    docker exec wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "\dt" | head -30
else
    echo "❌ Ошибка при восстановлении базы данных"
    exit 1
fi
