# Тестовые данные для WMSDIPL

## Состояние базы данных

Данные очищены и созданы новые тестовые приходы.

### Текущие приходы в базе

| ID | Номер документа | Дата | Поставщик | Статус | Строк |
|----|----------------|------|-----------|--------|-------|
| 25 | RECEIPT-2026-001 | 2026-01-08 | SUPPLIER-ALPHA | DRAFT | 5 |
| 26 | RECEIPT-2026-002 | 2026-01-08 | SUPPLIER-FOODCO | DRAFT | 4 |
| 27 | RECEIPT-2026-003 | 2026-01-08 | SUPPLIER-TECHMART | DRAFT | 3 |

### Детали приходов

#### RECEIPT-2026-001 (Электроника - общее оборудование)
- Ноутбук Dell XPS 15 - 25 шт
- Монитор Samsung 27" - 40 шт
- Мышь Logitech MX Master 3 - 100 шт
- Клавиатура Keychron K8 - 75 шт
- Наушники Sony WH-1000XM5 - 50 шт

#### RECEIPT-2026-002 (Продукты питания с датами годности)
- Молоко 3.2% 1л - 200 л (годен до 2026-01-22)
- Хлеб белый нарезной - 150 шт (годен до 2026-01-10)
- Сыр российский 45% - 80 кг (годен до 2026-02-15)
- Йогурт натуральный 2.5% - 300 шт (годен до 2026-01-18)

#### RECEIPT-2026-003 (Электроника - Apple продукты)
- Смартфон iPhone 15 Pro - 30 шт
- Планшет iPad Pro 12.9" - 20 шт
- Умные часы Apple Watch Ultra - 15 шт

## XML файлы приходов

Все XML файлы находятся в `import-service/input/loaded/`:
- `receipt_20260108_001.xml` - общая электроника
- `receipt_20260108_002_food.xml` - продукты питания
- `receipt_20260108_003_tech.xml` - Apple продукты

## Тестирование рабочего процесса

### 1. Просмотр приходов через API

```bash
# Получить все приходы
curl http://localhost:8080/api/receipts

# Получить конкретный приход
curl http://localhost:8080/api/receipts/25
```

### 2. Подтверждение прихода (DRAFT → CONFIRMED)

```bash
curl -X POST http://localhost:8080/api/receipts/25/confirm
```

### 3. Начало приемки (CONFIRMED → RECEIVING)

```bash
curl -X POST http://localhost:8080/api/receipts/25/start-receiving \
  -H "Content-Type: application/json" \
  -d '{"taskCount": 2}'
```

### 4. Приемка товара (создание паллет)

```bash
curl -X POST http://localhost:8080/api/receipts/25/receive-pallet \
  -H "Content-Type: application/json" \
  -d '{
    "palletCode": "PLT-20260108-001",
    "quantity": 25,
    "sscc": "00012345678901234567",
    "taskId": 1
  }'
```

### 5. Завершение приемки (RECEIVING → ACCEPTED)

```bash
curl -X POST http://localhost:8080/api/receipts/25/complete-receiving
```

### 6. Начало размещения (ACCEPTED → PLACING)
**Автоматически генерирует задачи размещения!**

```bash
curl -X POST http://localhost:8080/api/receipts/25/start-placement
```

### 7. Размещение паллеты
**При завершении последней задачи автоматически переводит приход в STOCKED!**

```bash
curl -X POST http://localhost:8080/api/receipts/25/record-placement \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": 5,
    "palletCode": "PLT-20260108-001",
    "quantity": 25,
    "sscc": "00012345678901234567",
    "targetLocationCode": "A-01-01"
  }'
```

## Создание новых тестовых XML файлов

Чтобы создать новый тестовый приход, просто поместите XML файл в `import-service/input/`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<receipt messageId="msg-unique-id" docNo="DOC-NUMBER" docDate="2026-01-08" supplier="SUPPLIER-NAME">
  <line lineNo="1" sku="SKU-CODE" name="Product Name" uom="шт" qtyExpected="100" packaging="коробка" sscc="00012345678901234567"/>
</receipt>
```

Файл будет автоматически обработан import-service и перемещен в `loaded/`.

## Очистка данных

Для очистки всех данных используйте следующие SQL команды:

```bash
docker compose exec -T postgres psql -U wmsdipl -d wmsdipl -c "
DELETE FROM scans;
DELETE FROM pallet_movements;
DELETE FROM tasks;
DELETE FROM pallets;
DELETE FROM receipt_lines;
DELETE FROM receipts;
"
```

## Текущая статистика

```
receipts:         3 записи
receipt_lines:    12 строк (5+4+3)
pallets:          0 (еще не создано)
tasks:            0 (еще не создано)
pallet_movements: 0
scans:            0
```

## Новые возможности (реализовано в январе 2026)

### ✅ Автоматические переходы статусов при размещении

1. **Автоматическая генерация задач размещения**
   - При вызове `POST /api/receipts/{id}/start-placement`
   - Система автоматически создает задачи размещения через PutawayService
   - Если задачи созданы → статус автоматически меняется на PLACING

2. **Автоматическое завершение размещения**
   - При выполнении последней задачи размещения (через `record-placement`)
   - Система проверяет, все ли задачи PLACEMENT завершены
   - Если да → автоматически переводит приход в статус STOCKED

Это избавляет от необходимости вручную вызывать `/complete-placement`!
