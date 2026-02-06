# XML Schema Visualization

```
📄 receipt.xml
│
└── <receipt> ──────────────────────────── Корневой элемент
    │
    ├── @messageId (required)              Уникальный ID сообщения
    ├── @docNo (required)                  Номер документа прихода
    ├── @docDate (optional)                Дата документа (YYYY-MM-DD)
    └── @supplier (optional)               Наименование поставщика
    │
    └── <line> ─────────────────────────── Строка прихода (0..N)
        │
        ├── @lineNo (optional)             Номер строки
        ├── @sku (optional)                Артикул товара
        ├── @name (optional)               Наименование товара
        ├── @uom (optional)                Единица измерения
        ├── @qtyExpected (optional)        Ожидаемое количество
        ├── @packaging (optional)          Тип упаковки
        └── @sscc (optional)               SSCC код (18 цифр)
```

## Пример минимального документа

```xml
<?xml version="1.0" encoding="UTF-8"?>
<receipt messageId="MSG-001" docNo="RCP-001">
    <line sku="SKU-001" qtyExpected="100"/>
</receipt>
```

## Пример полного документа

```xml
<?xml version="1.0" encoding="UTF-8"?>
<receipt messageId="MSG-001" 
         docNo="RCP-001" 
         docDate="2026-01-12" 
         supplier="Supplier Inc">
    
    <line lineNo="1" 
          sku="SKU-001" 
          name="Product Name" 
          uom="шт" 
          qtyExpected="100" 
          packaging="коробка" 
          sscc="001234567890123456"/>
    
    <line lineNo="2" 
          sku="SKU-002" 
          name="Product 2" 
          uom="кг" 
          qtyExpected="50.5" 
          packaging="ящик" 
          sscc="001234567890123457"/>
</receipt>
```

## Поток обработки

```
┌─────────────┐
│  XML файл   │
│ (incoming/) │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  XmlParser  │ ◄── Парсинг + валидация
└──────┬──────┘
       │
       ▼
┌──────────────┐
│ImportPayload │ ◄── DTO объект
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ ImportClient │ ◄── POST /api/imports
└──────┬───────┘
       │
       ▼
┌──────────────┐
│   Receipt    │ ◄── Создание прихода в БД
│  (DRAFT)     │     со статусом DRAFT
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ processed/   │ ◄── Перемещение файла
│   или        │
│  failed/     │
└──────────────┘
```

## Типы данных

| Поле | Java тип | SQL тип | Пример |
|------|----------|---------|--------|
| messageId | String | VARCHAR | `MSG-2026-01-001` |
| docNo | String | VARCHAR | `RCP-2026-001` |
| docDate | LocalDate | DATE | `2026-01-12` |
| supplier | String | VARCHAR | `ООО Поставщик` |
| lineNo | Integer | INTEGER | `1`, `2`, `3` |
| sku | String | VARCHAR | `SKU-001` |
| name | String | VARCHAR | `Молоко 3.2%` |
| uom | String | VARCHAR | `шт`, `кг`, `л` |
| qtyExpected | BigDecimal | DECIMAL | `100`, `45.5` |
| packaging | String | VARCHAR | `коробка`, `ящик` |
| sscc | String | VARCHAR(18) | `001234567890123456` |

## Mapping: XML → ImportPayload → Receipt

```
XML Element              ImportPayload           Receipt Entity
─────────────────────────────────────────────────────────────
<receipt>
  @messageId         →   messageId           →   messageId
  @docNo             →   docNo               →   documentNumber
  @docDate           →   docDate             →   documentDate
  @supplier          →   supplier            →   supplierName
  
  <line>
    @lineNo          →   Line.lineNo         →   ReceiptLine.lineNumber
    @sku             →   Line.sku            →   ReceiptLine.sku.code
    @name            →   Line.name           →   ReceiptLine.description
    @uom             →   Line.uom            →   ReceiptLine.unitOfMeasure
    @qtyExpected     →   Line.qtyExpected    →   ReceiptLine.expectedQty
    @packaging       →   Line.packaging      →   ReceiptLine.packagingType
    @sscc            →   Line.sscc           →   ReceiptLine.sscc
```
