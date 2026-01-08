# ✅ ИСПРАВЛЕНА: Ошибка с паллетами

## Проблема
При открытии экрана "Паллеты" в Desktop Client и нажатии кнопки "Обновить" появлялась ошибка:
```
Ошибка: java.lang.RuntimeException: java.io.IOException: 
Unexpected status: 500 body={"timestamp":"2026-01-08T12:49:04.603+00:00",
"status":500,"error":"Internal Server Error","path":"/api/pallets"}
```

## Причина
`PalletController.getAll()` возвращал **сырые Pallet entities** напрямую.

Pallet entity содержит lazy-loaded поля:
- `location` (Location entity)
- `receipt` (Receipt entity)
- `receiptLine` (ReceiptLine entity)

При попытке сериализации Jackson пытался загрузить эти связанные объекты, но натыкался на Hibernate proxy (`ByteBuddyInterceptor`), который не может быть сериализован → HTTP 500.

**Ошибка из лога**:
```
No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor
...["location"]->...["hibernateLazyInitializer"]
```

## Решение
Применён тот же паттерн, что и для Scan:

### Шаг 1: Создан PalletDto
**Файл**: `shared-contracts/src/main/java/com/wmsdipl/contracts/dto/PalletDto.java`

```java
public record PalletDto(
    Long id,
    String code,
    String codeType,
    String status,
    Long skuId,
    String uom,
    BigDecimal quantity,
    Long locationId,
    String locationCode,    // ✅ код локации вместо всего объекта
    Long receiptId,
    Long receiptLineId,
    String lotNumber,
    LocalDate expiryDate,
    BigDecimal weightKg,
    BigDecimal heightCm,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

**Преимущества**:
- ✅ Нет lazy-loaded связей
- ✅ Только ID и простые поля
- ✅ Легко сериализуется в JSON
- ✅ Включен `locationCode` для удобства

### Шаг 2: Создан PalletMapper
**Файл**: `core-api/src/main/java/com/wmsdipl/core/mapper/PalletMapper.java`

```java
@Component
public class PalletMapper {
    public PalletDto toDto(Pallet pallet) {
        return new PalletDto(
            pallet.getId(),
            pallet.getCode(),
            pallet.getCodeType(),
            pallet.getStatus() != null ? pallet.getStatus().name() : null,
            pallet.getSkuId(),
            pallet.getUom(),
            pallet.getQuantity(),
            pallet.getLocation() != null ? pallet.getLocation().getId() : null,
            pallet.getLocation() != null ? pallet.getLocation().getCode() : null,
            pallet.getReceipt() != null ? pallet.getReceipt().getId() : null,
            pallet.getReceiptLine() != null ? pallet.getReceiptLine().getId() : null,
            pallet.getLotNumber(),
            pallet.getExpiryDate(),
            pallet.getWeightKg(),
            pallet.getHeightCm(),
            pallet.getCreatedAt(),
            pallet.getUpdatedAt()
        );
    }
}
```

**Особенности**:
- ✅ Безопасная обработка null (null checks)
- ✅ Извлекает только ID из связанных объектов
- ✅ Enum → String конвертация (status)

### Шаг 3: Обновлён PalletController
**Файл**: `core-api/src/main/java/com/wmsdipl/core/web/PalletController.java`

**Было**:
```java
@GetMapping
public List<Pallet> getAll() {
    return palletService.getAll();
}
```

**Стало**:
```java
@GetMapping
public List<PalletDto> getAll() {
    return palletService.getAll().stream()
            .map(palletMapper::toDto)
            .collect(Collectors.toList());
}
```

**Изменения**:
- ✅ Добавлен `PalletMapper` в конструктор
- ✅ `getAll()` возвращает `List<PalletDto>`
- ✅ `getById()` возвращает `ResponseEntity<PalletDto>`
- ✅ Все entity → DTO конвертируются через mapper

## Результат

### Тест API:
```bash
curl -u testuser:password http://localhost:8080/api/pallets
```

**Ответ** (HTTP 200 ✅):
```json
[
  {
    "id": 1,
    "code": "PLT-TEST-001",
    "codeType": "INTERNAL",
    "status": "RECEIVING",
    "skuId": 2,
    "uom": null,
    "quantity": 20.000,
    "locationId": 1,
    "locationCode": "TEST-RCV-001",
    "receiptId": 14,
    "receiptLineId": 8,
    "lotNumber": null,
    "expiryDate": null,
    "weightKg": null,
    "heightCm": null,
    "createdAt": "2026-01-08T04:53:05.593855",
    "updatedAt": "2026-01-08T15:50:37.499755"
  },
  ...
]
```

### Desktop Client:
1. Открыть "Паллеты"
2. Нажать "Обновить"
3. ✅ Таблица заполняется без ошибок

**Колонки отображаются**:
- code
- status
- location (locationCode)
- skuId
- qty (quantity)
- receiptId

## Аналогичные исправления

Эта же проблема была исправлена ранее для:
- ✅ `TaskController.scan()` - возвращает ScanDto вместо Scan
- ✅ `TaskController.getTaskScans()` - возвращает List<ScanDto>

**Правило**: **Контроллеры НЕ должны возвращать JPA entities напрямую!**

Всегда использовать:
1. Создать DTO в `shared-contracts/src/main/java/com/wmsdipl/contracts/dto/`
2. Создать Mapper в `core-api/src/main/java/com/wmsdipl/core/mapper/`
3. Внедрить Mapper в Controller
4. Конвертировать Entity → DTO перед возвратом

## Статус

✅ **Проблема полностью решена**
✅ **Core API перезапущен**
✅ **Endpoint /api/pallets работает**
✅ **Desktop Client готов к работе**

---

**Дата исправления**: 8 января 2026  
**Время**: 16:10 UTC+3
