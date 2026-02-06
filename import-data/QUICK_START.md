# XML Import Quick Reference

## Минимальный XML шаблон

```xml
<?xml version="1.0" encoding="UTF-8"?>
<receipt messageId="UNIQUE-ID" docNo="DOC-NUMBER" docDate="2026-01-12">
    <line sku="SKU-001" name="Product Name" uom="шт" qtyExpected="100"/>
</receipt>
```

## Полный XML шаблон

```xml
<?xml version="1.0" encoding="UTF-8"?>
<receipt messageId="MSG-ID" 
         docNo="DOC-NO" 
         docDate="2026-01-12" 
         supplier="Supplier Name">
    <line lineNo="1" 
          sku="SKU-001" 
          name="Product Name" 
          uom="шт" 
          qtyExpected="100" 
          packaging="box" 
          sscc="001234567890123456"/>
</receipt>
```

## Быстрый старт

1. **Скопируйте шаблон** → `receipt-test.xml`
2. **Измените** `messageId` и `docNo` на уникальные
3. **Добавьте** строки товаров
4. **Скопируйте** в `import-data/incoming/`
5. **Проверьте** результат в desktop-client → "Приходы"

## Поля (✅ обязательные, ❌ опциональные)

### Receipt
- ✅ `messageId` - уникальный ID
- ✅ `docNo` - номер документа
- ❌ `docDate` - дата (YYYY-MM-DD)
- ❌ `supplier` - поставщик

### Line
- ❌ `lineNo` - номер строки
- ❌ `sku` - артикул
- ❌ `name` - название
- ❌ `uom` - ед. изм.
- ❌ `qtyExpected` - количество
- ❌ `packaging` - упаковка
- ❌ `sscc` - штрихкод (18 цифр)

## Примеры значений

```xml
<!-- Целые числа -->
qtyExpected="100"

<!-- Дробные числа -->
qtyExpected="45.5"

<!-- Дата ISO -->
docDate="2026-01-12"

<!-- SSCC код -->
sscc="001234567890123456"
```

## Команды

```bash
# Импорт
cp examples/receipt-simple.xml incoming/

# Очистка
rm -f incoming/*.xml

# Просмотр
cat examples/receipt-simple.xml
```
