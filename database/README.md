# База данных WMSDIPL

## 📂 Структура папки

- `init_schema.sql` - **Основной файл схемы БД** (используйте этот!)
- `schema.sql` - Автоматический дамп от pg_dump (для справки)
- `restore_database.bat` - Скрипт восстановления для Windows
- `restore_database.sh` - Скрипт восстановления для Linux/Mac
- `backup_data.sql` - Дамп данных (создается вручную при необходимости)

## 🚀 Быстрое восстановление базы

### Windows
```batch
cd E:\WMSDIPL
database\restore_database.bat
```

### Linux/Mac
```bash
cd /path/to/WMSDIPL
chmod +x database/restore_database.sh
./database/restore_database.sh
```

### Вручную (любая ОС)
```bash
# 1. Пересоздать базу данных
docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "DROP DATABASE IF EXISTS wmsdipl;"
docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "CREATE DATABASE wmsdipl;"

# 2. Восстановить схему
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl < database/init_schema.sql
```

## 💾 Создание бэкапа

### Только схема (структура таблиц)
```bash
docker exec wmsdipl-postgres pg_dump -U wmsdipl -d wmsdipl --schema-only --no-owner --no-acl > database/schema_backup_$(date +%Y%m%d).sql
```

### Только данные
```bash
docker exec wmsdipl-postgres pg_dump -U wmsdipl -d wmsdipl --data-only --no-owner --no-acl > database/data_backup_$(date +%Y%m%d).sql
```

### Полный дамп (схема + данные)
```bash
docker exec wmsdipl-postgres pg_dump -U wmsdipl -d wmsdipl --no-owner --no-acl > database/full_backup_$(date +%Y%m%d).sql
```

## 📋 Структура базы данных

### Основные таблицы

**Справочники:**
- `zones` - Зоны склада
- `locations` - Ячейки хранения
- `skus` - Товары (SKU)
- `packagings` - Упаковки
- `users` - Пользователи

**Приходы:**
- `receipts` - Документы приходов
- `receipt_lines` - Строки приходов

**Складские операции:**
- `pallets` - Паллеты
- `tasks` - Задания (приемка, размещение, отбор)
- `scans` - Сканирования
- `discrepancies` - Расхождения

**Конфигурация:**
- `import_config` - Настройки импорта
- `putaway_rules` - Правила размещения
- `sku_storage_config` - Конфигурация хранения SKU

**Аудит:**
- `audit_logs` - Журнал изменений
- `status_history` - История статусов
- `pallet_movements` - Движения паллет
- `import_log` - Лог импорта

## ⚠️ Важно: Миграции отключены!

В этом проекте **Flyway миграции отключены**. Вместо этого используется единый файл схемы `init_schema.sql`.

### Почему?

1. ✅ **Проще в управлении** - один файл вместо 20+ миграций
2. ✅ **Быстрое восстановление** - один скрипт вместо применения миграций по порядку
3. ✅ **Нет проблем с версионированием** - не нужно следить за порядком миграций
4. ✅ **Подходит для малых проектов** - один разработчик, локальная разработка

### Как обновлять схему?

1. Внесите изменения прямо в `init_schema.sql`
2. Запустите `restore_database.bat` для применения изменений
3. Зафиксируйте изменения в Git

### Отключение Flyway

Для **полного отключения** Flyway в приложениях добавьте в `application.yml`:

```yaml
spring:
  flyway:
    enabled: false
```

**Или удалите папки миграций:**
- `core-api/src/main/resources/db/migration/`
- `import-service/src/main/resources/db/migration/`

## 🔄 Типичный рабочий процесс

### Начало работы
```bash
docker compose up -d postgres
database\restore_database.bat
gradle :core-api:bootRun
```

### Изменение схемы
1. Отредактируйте `database/init_schema.sql`
2. Запустите `database\restore_database.bat`
3. Перезапустите приложения

### Сброс базы к чистому состоянию
```bash
database\restore_database.bat
```

## 📝 Git рекомендации

**.gitignore:**
```
# Игнорировать временные бэкапы
database/*_backup_*.sql
database/schema.sql

# Сохранять основной файл схемы
!database/init_schema.sql
!database/*.bat
!database/*.sh
!database/README.md
```

## 🎯 Тестовые данные

Если нужны тестовые данные, создайте файл `database/test_data.sql`:

```sql
-- Зоны
INSERT INTO zones (code, name, priority_rank) VALUES 
('ZONE-A', 'Зона А - Быстрый доступ', 1),
('ZONE-B', 'Зона Б - Хранение', 2),
('ZONE-C', 'Зона В - Резерв', 3);

-- Локации
INSERT INTO locations (code, zone_id, location_type, status, aisle, bay, level) VALUES 
('A-01-01-01', 1, 'STORAGE', 'AVAILABLE', 'A-01', '01', '01'),
('A-01-01-02', 1, 'STORAGE', 'AVAILABLE', 'A-01', '01', '02');

-- SKU
INSERT INTO skus (code, name, uom, pallet_capacity) VALUES 
('SKU-001', 'Тестовый товар 1', 'ШТ', 100),
('SKU-002', 'Тестовый товар 2', 'ШТ', 50);
```

Запуск тестовых данных:
```bash
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl < database/test_data.sql
```

## 🆘 Troubleshooting

### База не восстанавливается
```bash
# Проверить, что контейнер работает
docker ps | grep wmsdipl-postgres

# Проверить логи
docker logs wmsdipl-postgres

# Полный рестарт
docker compose down -v
docker compose up -d postgres
database\restore_database.bat
```

### "relation already exists"
Схема пытается создать таблицы, которые уже существуют. Решение:
```bash
# Полностью удалить базу и создать заново
docker compose down -v
docker compose up -d postgres
database\restore_database.bat
```

### Flyway пытается применить миграции
Отключите Flyway в `application.yml`:
```yaml
spring:
  flyway:
    enabled: false
```
