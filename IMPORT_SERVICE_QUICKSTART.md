# Быстрый Тест Import Service

## Шаг 1: Запуск сервисов

### Автоматический запуск (рекомендуется)
```cmd
start-all.bat
```

### Или по отдельности
```cmd
# 1. PostgreSQL
start-postgres.bat

# 2. Core API
start-core-api.bat
(подождать 10 секунд)

# 3. Import Service
start-import-service.bat
```

---

## Шаг 2: Проверка статуса

```cmd
# Проверить Core API
curl http://localhost:8080/actuator/health

# Проверить Import Service
curl http://localhost:8090/actuator/health

# Проверить PostgreSQL
docker ps | findstr wmsdipl-postgres
```

**Ожидаемый результат**: Все сервисы отвечают `{"status":"UP"}`

---

## Шаг 3: Подготовка тестового файла

### Скопировать шаблон в папку импорта
```cmd
# Создать папку input (если не существует)
mkdir input

# Скопировать тестовый файл
copy docs\import-template.xml input\test-receipt-001.xml
```

---

## Шаг 4: Наблюдение за импортом

### Что должно произойти:
1. **Import Service** обнаружит файл в `input/` (через 2 секунды)
2. Распарсит XML и вычислит `messageId`
3. Отправит POST запрос в Core API: `/api/imports`
4. Core API создаст накладную в статусе `DRAFT`
5. Import Service переместит файл в `processed/` или `failed/`

### Проверить логи Import Service
Откройте окно консоли Import Service и найдите строки типа:
```
Processing file: test-receipt-001.xml
MessageId: xxxxx
Import successful: Receipt created with ID: 123
```

---

## Шаг 5: Проверка результата

### Вариант 1: Desktop Client
```cmd
start-desktop-client.bat

# Войти: testuser / password
# Перейти в раздел "Поступления"
# Нажать "Обновить"
# Должна появиться новая накладная
```

### Вариант 2: API
```cmd
curl -u testuser:password http://localhost:8080/api/receipts
```

### Вариант 3: База данных
```cmd
docker exec -it wmsdipl-postgres psql -U wmsdipl -d wmsdipl

SELECT id, doc_no, message_id, status, created_at 
FROM receipts 
ORDER BY created_at DESC 
LIMIT 5;
```

---

## Шаг 6: Тест идемпотентности

### Импортировать тот же файл повторно
```cmd
copy docs\import-template.xml input\test-receipt-002.xml
```

**Ожидаемый результат**:
- Import Service обработает файл
- Core API вернет HTTP 200 (не 201)
- Накладная НЕ создастся повторно (тот же `messageId`)
- В логах: "Receipt already exists with messageId: xxxxx"

---

## Параметры Import Service

### По умолчанию
- **Порт**: 8090
- **Папка импорта**: `input/`
- **Интервал проверки**: 2000 мс (2 секунды)
- **Core API URL**: http://localhost:8080

### Изменить параметры
```cmd
# Изменить папку импорта
set WMS_IMPORT_FOLDER=C:\my-import-folder
start-import-service.bat

# Изменить интервал проверки (в миллисекундах)
set WMS_IMPORT_POLL_MS=5000
start-import-service.bat

# Изменить URL Core API
set WMS_CORE_API_BASE=http://192.168.1.100:8080
start-import-service.bat
```

---

## Структура XML файла

Пример минимального XML для импорта (`docs/import-template.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<receipt messageId="MSG-001" docNo="RCV-2026-001" supplier="Test Supplier">
    <lines>
        <line lineNo="1" uom="PCS" qtyExpected="100"/>
    </lines>
</receipt>
```

### Обязательные поля:
- `messageId` — уникальный идентификатор (для идемпотентности)
- `docNo` — номер документа
- `lines` — минимум одна строка с `lineNo`, `uom`, `qtyExpected`

---

## Расположение файлов

```
E:\WMSDIPL\
├── input/                  ← Сюда кладем XML файлы
├── processed/              ← Сюда перемещаются обработанные файлы (если настроено)
├── failed/                 ← Сюда перемещаются файлы с ошибками (если настроено)
└── docs/
    └── import-template.xml ← Шаблон для тестирования
```

---

## Типичные Проблемы

### Import Service не видит файлы
**Решение**:
1. Проверьте папку: `dir input`
2. Проверьте логи Import Service
3. Убедитесь, что сервис запущен: `netstat -ano | findstr :8090`

### Ошибка "Connection refused" при обращении к Core API
**Решение**:
1. Проверьте, что Core API запущен: `curl http://localhost:8080/actuator/health`
2. Если не запущен, запустите: `start-core-api.bat`

### Ошибка валидации XML
**Решение**:
1. Проверьте формат XML файла
2. Убедитесь, что все обязательные поля заполнены
3. Проверьте кодировку файла (должна быть UTF-8)

### Дубликат messageId
**Поведение**: Это нормально! Идемпотентность работает.
- HTTP 200 (не 201)
- Накладная не создается повторно
- Возвращается существующая накладная

---

## Мониторинг Import Service

### Проверить, какие файлы обрабатываются
Смотрите логи в окне консоли Import Service.

### Проверить историю импорта в БД
```sql
docker exec -it wmsdipl-postgres psql -U wmsdipl -d wmsdipl

SELECT * FROM import_log 
ORDER BY created_at DESC 
LIMIT 10;
```

---

## Остановка

```cmd
# Остановить все сервисы
stop-all.bat

# Или только Import Service
# Найти процесс
netstat -ano | findstr :8090
# Остановить (замените <PID>)
taskkill /F /PID <PID>
```

---

## Быстрая Проверка (30 секунд)

```cmd
# 1. Запустить все
start-all.bat

# 2. Подождать 20 секунд

# 3. Скопировать тестовый файл
copy docs\import-template.xml input\test.xml

# 4. Подождать 3 секунды

# 5. Проверить накладные
curl -u testuser:password http://localhost:8080/api/receipts

# 6. Должна появиться новая накладная с docNo из XML
```

---

**Готово!** Import Service настроен и готов к работе.

Для подробной информации см. [BAT_FILES_GUIDE.md](BAT_FILES_GUIDE.md)
