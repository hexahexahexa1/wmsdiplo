# Feature Plan: Stock Inventory View & History

**Date**: 2026-01-11  
**Author**: AI Assistant  
**Status**: Draft  
**Related Issues**: N/A

## 1. Overview

### Business Goal
Создать механику СТОКА - систему отображения товаров на складе с полной информацией о паллетах, ячейках и историей движения. Обеспечить возможность просмотра текущего состояния склада и состояния на любую дату в прошлом (point-in-time view) для инвентаризаций, аудита и анализа движения товаров.

### User Story
**Как менеджер склада или инвентаризатор**, я хочу видеть все товары на складе с детальной информацией о паллетах и их расположении, **чтобы** контролировать остатки, проводить инвентаризации и отслеживать движение товаров.

### Acceptance Criteria
- [ ] Просмотр списка всех паллет на складе с фильтрацией по SKU, Location, Паллете, Приемке, Статусу
- [ ] Отображение детальной информации о каждой паллете (SKU, количество, ячейка, дата прихода, срок годности)
- [ ] Просмотр истории движений каждой паллеты в хронологическом порядке
- [ ] Возможность выбора среза времени (просмотр стока на конкретную дату)
- [ ] Пагинация и сортировка результатов
- [ ] JavaFX desktop экран "Остатки на складе" с фильтрами
- [ ] REST API для всех операций просмотра

## 2. Functional Requirements

### Inputs
- **Фильтры просмотра стока**:
  - `skuCode` (String) - код товара
  - `locationCode` (String) - код ячейки
  - `palletBarcode` (String) - штрих-код паллеты
  - `receiptId` (Long) - ID приемки
  - `status` (PalletStatus) - статус паллеты (RECEIVED, PLACED, etc.)
  - `asOfDate` (LocalDateTime, optional) - срез времени (если не указан - текущий момент)
- **Пагинация**: `page`, `size`, `sort`
- **Запрос истории**: `palletId` (Long)

### Outputs
- **Список паллет на складе** (StockItemDto):
  - palletId, palletCode, skuId, skuCode, skuName, uom
  - quantity, locationId, locationCode, locationZone
  - status, receivedAt, receiptDocNo, expiryDate, lotNumber
- **История движений паллеты** (List<StockMovementDto>):
  - movementId, movementType (RECEIVE, PLACE, MOVE, PICK)
  - fromLocation, toLocation, quantity, timestamp
  - taskType, performedBy, comment

### Business Rules
1. **Текущий сток**: паллеты с qty > 0 и статусом != SHIPPED
2. **Исторический сток (point-in-time)**: паллеты, существовавшие на момент asOfDate с их местоположением на тот момент
3. **Паллеты без размещения**: показываем со статусом RECEIVED, locationCode = null
4. **Частично отобранные паллеты**: показываем с текущим quantity (промежуточные изменения не отслеживаем)
5. **Пустые паллеты** (qty = 0): скрываем из стока
6. **Движения**: создаются при завершении RECEIVING (RECEIVE), PLACEMENT (PLACE), будущих MOVE и PICK задач

### Edge Cases
- **Scenario**: Паллета принята, но не размещена (status=RECEIVED, location=null)
  - **Handling**: Показываем в списке стока без адреса ячейки, locationCode = null
  
- **Scenario**: Паллета полностью отобрана (qty=0)
  - **Handling**: Скрываем из текущего стока, показываем в историческом если asOfDate < дата отбора
  
- **Scenario**: Запрошен срез времени до создания паллеты
  - **Handling**: Паллета не показывается в результатах
  
- **Scenario**: Паллета с несуществующим SKU (данных нет в справочнике)
  - **Handling**: Не должно происходить (FK constraint), но если случилось - показываем skuCode=null, skuName="<Unknown>"

- **Scenario**: Запрос стока на будущую дату (asOfDate > now)
  - **Handling**: Возвращаем ошибку 400 Bad Request "Cannot view stock in the future"

## 3. Technical Design

### Affected Modules
- [x] core-api (основная реализация)
- [x] shared-contracts (новые DTOs)
- [ ] import-service (не затрагивается)
- [x] desktop-client (новый экран "Остатки")

### Domain Model Changes

#### Existing Entities (No Changes Needed)
- `Pallet` (уже есть все поля):
  - id, code, codeType, status (PalletStatus enum)
  - location (ManyToOne), skuId, lotNumber, expiryDate
  - quantity, uom, receipt, receiptLine
  - createdAt, updatedAt
  
- `PalletMovement` (уже существует):
  - id, pallet (ManyToOne), fromLocation, toLocation
  - movementType (String), taskId, movedBy, movedAt

- `PalletStatus` enum (уже есть нужные значения):
  - EMPTY, RECEIVING, RECEIVED, STORED, IN_TRANSIT, PLACED, PICKING, SHIPPED

#### New Entities
**НЕТ** - все необходимые entity уже существуют!

#### Database Migrations

**НЕТ** - схема БД уже полностью готова. Таблицы `pallets` и `pallet_movements` уже созданы.

**Но нужно**: Мигрировать историю из существующих Task/Scan в PalletMovement для тестирования:
- Scan записи с taskType=RECEIVING → PalletMovement (type=RECEIVE)
- Task записи PLACEMENT (completed) → PalletMovement (type=PLACE)

#### Indexes (Проверить существующие и добавить при необходимости)
```sql
-- Для быстрого поиска по SKU
CREATE INDEX IF NOT EXISTS idx_pallet_sku_id ON pallets(sku_id);

-- Для фильтрации по ячейке
CREATE INDEX IF NOT EXISTS idx_pallet_location_id ON pallets(location_id);

-- Для фильтрации по приемке
CREATE INDEX IF NOT EXISTS idx_pallet_receipt_id ON pallets(receipt_id);

-- Для фильтрации по статусу
CREATE INDEX IF NOT EXISTS idx_pallet_status ON pallets(status);

-- Composite для точных запросов
CREATE INDEX IF NOT EXISTS idx_pallet_sku_location ON pallets(sku_id, location_id) WHERE quantity > 0;

-- Для истории движений
CREATE INDEX IF NOT EXISTS idx_movement_pallet_id ON pallet_movements(pallet_id);
CREATE INDEX IF NOT EXISTS idx_movement_moved_at ON pallet_movements(moved_at);
```

### API Design

#### New Endpoints

```
GET  /api/stock                          - Список стока с фильтрами (pageable)
GET  /api/stock/pallet/{id}              - Детали паллеты
GET  /api/stock/pallet/{id}/history      - История движений паллеты
GET  /api/stock/summary                  - Сводка по SKU (агрегированные остатки)
GET  /api/stock/location/{locationId}    - Что лежит в ячейке
```

#### Request Parameters (GET /api/stock)

```java
@RequestParam(required = false) String skuCode
@RequestParam(required = false) String locationCode
@RequestParam(required = false) String palletBarcode
@RequestParam(required = false) Long receiptId
@RequestParam(required = false) PalletStatus status
@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime asOfDate
@RequestParam(defaultValue = "0") int page
@RequestParam(defaultValue = "50") int size
@RequestParam(defaultValue = "createdAt,desc") String[] sort
```

#### Request DTOs
**НЕТ** - используем @RequestParam для фильтров

#### Response DTOs

```java
// shared-contracts/src/main/java/com/wmsdipl/contracts/dto/StockItemDto.java
public record StockItemDto(
    Long palletId,
    String palletCode,
    Long skuId,
    String skuCode,
    String skuName,
    String uom,
    BigDecimal quantity,
    Long locationId,
    String locationCode,
    String locationZone,      // zone.name
    String status,            // PalletStatus as String
    LocalDateTime receivedAt, // pallet.createdAt
    String receiptDocNo,
    LocalDate expiryDate,
    String lotNumber
) {}

// shared-contracts/src/main/java/com/wmsdipl/contracts/dto/StockMovementDto.java
public record StockMovementDto(
    Long id,
    String movementType,      // RECEIVE, PLACE, MOVE, PICK
    String fromLocationCode,
    String toLocationCode,
    BigDecimal quantity,      // для будущих частичных перемещений
    LocalDateTime timestamp,
    String taskType,          // RECEIVING, PLACEMENT, etc. (from Task)
    String performedBy,       // movedBy
    String comment            // для будущих комментариев
) {}

// shared-contracts/src/main/java/com/wmsdipl/contracts/dto/StockSummaryDto.java
public record StockSummaryDto(
    Long skuId,
    String skuCode,
    String skuName,
    String uom,
    BigDecimal totalQuantity,  // SUM(quantity)
    Integer palletCount,       // COUNT(*)
    Integer locationCount      // COUNT(DISTINCT location_id)
) {}
```

### Service Layer Design

#### New Services

**`StockService`** (com.wmsdipl.core.service.StockService):
- Responsibilities:
  - Получение списка стока с фильтрацией и пагинацией
  - Point-in-time view (срез на дату)
  - Получение деталей паллеты
  - Получение истории движений паллеты
  - Агрегированная сводка по SKU
  
- Methods:
  ```java
  Page<Pallet> getStock(StockFilterCriteria criteria, Pageable pageable)
  Pallet getPalletById(Long id)
  List<PalletMovement> getPalletHistory(Long palletId)
  List<StockSummaryItem> getStockSummary(String skuCode, LocalDateTime asOfDate)
  ```

**`StockMovementService`** (com.wmsdipl.core.service.StockMovementService):
- Responsibilities:
  - Создание записей движения при завершении задач
  - Интеграция с ReceivingWorkflowService, PlacementWorkflowService
  
- Methods:
  ```java
  PalletMovement recordReceive(Pallet pallet, Location location, String movedBy, Long taskId)
  PalletMovement recordPlacement(Pallet pallet, Location fromLocation, Location toLocation, String movedBy, Long taskId)
  void migrateLegacyMovements() // для миграции истории из Task/Scan
  ```

#### Modified Services

**`ReceivingWorkflowService`**:
- При завершении receiving задачи → вызвать `stockMovementService.recordReceive()`
- Обновить pallet.status = RECEIVED

**`PlacementWorkflowService`**:
- При завершении placement задачи → вызвать `stockMovementService.recordPlacement()`
- Обновить pallet.location, pallet.status = PLACED

#### Service Dependencies

```
StockController
  ├── StockService
  │   ├── PalletRepository (find, filter)
  │   ├── PalletMovementRepository (findByPallet)
  │   ├── SkuRepository (for SKU details)
  │   ├── LocationRepository (for location details)
  │   └── ReceiptRepository (for receipt details)
  └── StockMapper
      └── (DTO transformation)

StockMovementService
  ├── PalletMovementRepository (save)
  ├── PalletRepository (update status, location)
  └── TaskRepository (optional, for migration)

ReceivingWorkflowService
  └── StockMovementService (recordReceive)

PlacementWorkflowService
  └── StockMovementService (recordPlacement)
```

### Workflow & State Machine

#### Pallet Lifecycle States

```
[EMPTY] --create--> [RECEIVING] --scan-complete--> [RECEIVED] --placement--> [PLACED]
                                                                                 |
                                                                                 v
                                                                    [PICKING] --ship--> [SHIPPED]
                                                                         ^
                                                                         |
                                                                    [IN_TRANSIT] (for moves)
```

**State Transitions**:
- EMPTY → RECEIVING: при создании паллеты в приемке
- RECEIVING → RECEIVED: при завершении RECEIVING задачи → создать PalletMovement (type=RECEIVE)
- RECEIVED → PLACED: при завершении PLACEMENT задачи → создать PalletMovement (type=PLACE), обновить location
- PLACED → IN_TRANSIT: при начале перемещения (будущая функция)
- IN_TRANSIT → PLACED: при завершении перемещения → создать PalletMovement (type=MOVE)
- PLACED → PICKING: при начале отбора (будущая функция)
- PICKING → SHIPPED: при завершении отбора → создать PalletMovement (type=PICK), установить qty=0

#### Movement Types

```java
public enum MovementType {
    RECEIVE,    // Приемка (task=RECEIVING)
    PLACE,      // Размещение (task=PLACEMENT)
    MOVE,       // Перемещение (будущая функция)
    PICK,       // Отбор (будущая функция)
    ADJUST      // Корректировка (инвентаризация, будущая функция)
}
```

## 4. Error Handling

### Exception Mapping
- `ResponseStatusException(404, "Pallet not found")` - паллета не существует
- `ResponseStatusException(404, "SKU not found")` - SKU не существует
- `ResponseStatusException(404, "Location not found")` - ячейка не существует
- `ResponseStatusException(400, "Cannot view stock in the future")` - asOfDate > now
- `IllegalArgumentException` - невалидные параметры фильтрации

### Error Response Format
```json
{
  "timestamp": "2026-01-11T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Pallet not found: 123",
  "path": "/api/stock/pallet/123"
}
```

## 5. Testing Strategy

### Unit Tests

**StockServiceTest**:
- [ ] `shouldReturnAllStock_WhenNoFiltersApplied()`
- [ ] `shouldFilterStockBySku_WhenSkuCodeProvided()`
- [ ] `shouldFilterStockByLocation_WhenLocationCodeProvided()`
- [ ] `shouldFilterStockByPalletBarcode_WhenBarcodeProvided()`
- [ ] `shouldFilterStockByReceipt_WhenReceiptIdProvided()`
- [ ] `shouldFilterStockByStatus_WhenStatusProvided()`
- [ ] `shouldReturnHistoricalStock_WhenAsOfDateProvided()`
- [ ] `shouldExcludeEmptyPallets_WhenQuantityIsZero()`
- [ ] `shouldIncludePalletsWithoutLocation_WhenStatusIsReceived()`
- [ ] `shouldReturnPalletHistory_WhenPalletIdValid()`
- [ ] `shouldThrowException_WhenPalletNotFound()`
- [ ] `shouldReturnStockSummary_WhenSkuCodeProvided()`

**StockMovementServiceTest**:
- [ ] `shouldRecordReceiveMovement_WhenReceivingCompleted()`
- [ ] `shouldRecordPlacementMovement_WhenPlacementCompleted()`
- [ ] `shouldUpdatePalletStatus_WhenMovementRecorded()`
- [ ] `shouldUpdatePalletLocation_WhenPlacementRecorded()`

**StockMapperTest**:
- [ ] `shouldMapPalletToStockItemDto_WhenAllFieldsPresent()`
- [ ] `shouldMapPalletToStockItemDto_WhenLocationIsNull()`
- [ ] `shouldMapPalletMovementToDto_WhenAllFieldsPresent()`

### Integration Tests

**StockControllerTest** (MockMvc):
- [ ] `shouldReturnStockList_WhenNoFilters()`
- [ ] `shouldReturnFilteredStock_WhenSkuCodeProvided()`
- [ ] `shouldReturnFilteredStock_WhenLocationCodeProvided()`
- [ ] `shouldReturnPaginatedStock_WhenPageSizeSpecified()`
- [ ] `shouldReturnSortedStock_WhenSortParameterProvided()`
- [ ] `shouldReturn404_WhenPalletNotFound()`
- [ ] `shouldReturnPalletHistory_WhenValidPalletId()`
- [ ] `shouldReturn400_WhenAsOfDateInFuture()`

**StockWorkflowIntegrationTest** (Testcontainers):
- [ ] `shouldCreateReceiveMovement_WhenReceivingTaskCompleted()`
- [ ] `shouldCreatePlacementMovement_WhenPlacementTaskCompleted()`
- [ ] `shouldShowPalletInStock_AfterReceiving()`
- [ ] `shouldUpdateLocationInStock_AfterPlacement()`
- [ ] `shouldShowHistoricalStock_WhenAsOfDateBeforePlacement()`

### Test Data
- Создать фикстуры:
  - 5 SKU (SKU001-SKU005)
  - 10 Location (A1-01-01, A1-01-02, etc.)
  - 20 Pallet с разными статусами и датами
  - 50 PalletMovement записей

## 6. Implementation Checklist

### Phase 1: Data Model & Migrations
- [ ] Проверить существующие индексы в БД
- [ ] Создать миграцию для добавления индексов (V020__add_stock_indexes.sql)
- [ ] Создать MovementType enum (если нужен, или использовать String)
- [ ] Проверить существующие Pallet, PalletMovement entities (готовы!)

### Phase 2: Service Layer
- [ ] Создать DTOs в shared-contracts:
  - [ ] StockItemDto
  - [ ] StockMovementDto
  - [ ] StockSummaryDto
- [ ] Создать StockService
  - [ ] Метод getStock() с фильтрацией и пагинацией
  - [ ] Метод getStock() с point-in-time (asOfDate)
  - [ ] Метод getPalletById()
  - [ ] Метод getPalletHistory()
  - [ ] Метод getStockSummary()
- [ ] Создать StockMovementService
  - [ ] Метод recordReceive()
  - [ ] Метод recordPlacement()
  - [ ] Метод migrateLegacyMovements()
- [ ] Написать unit тесты для сервисов

### Phase 3: API Layer
- [ ] Создать StockMapper (Pallet → StockItemDto, PalletMovement → StockMovementDto)
- [ ] Создать StockController
  - [ ] GET /api/stock (с фильтрами и пагинацией)
  - [ ] GET /api/stock/pallet/{id}
  - [ ] GET /api/stock/pallet/{id}/history
  - [ ] GET /api/stock/summary
  - [ ] GET /api/stock/location/{locationId}
- [ ] Добавить Swagger/OpenAPI аннотации
- [ ] Написать MockMvc тесты для контроллеров

### Phase 4: Integration with Workflows
- [ ] Обновить ReceivingWorkflowService
  - [ ] Вызвать stockMovementService.recordReceive() при завершении
  - [ ] Обновить pallet.status = RECEIVED
- [ ] Обновить PlacementWorkflowService
  - [ ] Вызвать stockMovementService.recordPlacement() при завершении
  - [ ] Обновить pallet.location и pallet.status = PLACED
- [ ] Написать integration тесты для полного цикла
- [ ] Запустить миграцию истории (migrateLegacyMovements)

### Phase 5: Desktop Client
- [ ] Создать StockView.fxml (экран "Остатки на складе")
- [ ] Создать StockViewController (JavaFX controller)
  - [ ] Таблица с паллетами (все колонки из StockItemDto)
  - [ ] Фильтры (SKU, Location, Pallet, Receipt, Status)
  - [ ] Выбор даты среза (DatePicker для asOfDate)
  - [ ] Кнопки: "Обновить", "Очистить фильтры", "История" (для выбранной паллеты)
- [ ] Интеграция с REST API (RestTemplate/HttpClient)
- [ ] Модальное окно для отображения истории движений

### Phase 6: Documentation
- [ ] Обновить API документацию (Swagger UI доступен)
- [ ] Обновить ER diagram (если были изменения)
- [ ] Обновить AGENTS.md (новые сервисы и endpoints)
- [ ] Создать USER_GUIDE.md для экрана "Остатки"

## 7. Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| N+1 query при загрузке стока с SKU/Location | High | High | Использовать JOIN FETCH или @EntityGraph в запросах |
| Медленная работа при больших объемах (>10k паллет) | Medium | Medium | Обязательная пагинация, индексы на все поля фильтрации |
| Некорректная миграция истории из Task/Scan | Medium | Low | Тщательное тестирование миграции, возможность rollback |
| Point-in-time view сложен для реализации | Medium | Low | Использовать WHERE movedAt <= asOfDate для фильтрации движений |
| Несогласованность между Pallet.location и последней записью PalletMovement | High | Low | Всегда обновлять оба при движении в одной транзакции |
| Конкурентная модификация паллеты (race condition) | Medium | Low | Использовать optimistic locking (@Version в Pallet) |

## 8. Performance Considerations

- **Database Indexes**: 
  - `idx_pallet_sku_id` - для фильтрации по товару
  - `idx_pallet_location_id` - для фильтрации по ячейке
  - `idx_pallet_receipt_id` - для фильтрации по приемке
  - `idx_pallet_status` - для фильтрации по статусу
  - `idx_pallet_sku_location` (composite) - для точных запросов
  - `idx_movement_pallet_id` - для истории паллеты
  - `idx_movement_moved_at` - для point-in-time запросов

- **Query Optimization**: 
  - JOIN FETCH для загрузки SKU, Location, Receipt за один запрос
  - @EntityGraph для избежания N+1
  - Использовать Specification API для динамических фильтров

- **Caching**: 
  - НЕ кэшировать список стока (меняется часто)
  - Кэшировать справочники SKU, Location (@Cacheable на findById)
  - Cache-Aside pattern для getSummary() с TTL 5 минут

- **Pagination**: 
  - Обязательна! Default page size = 50
  - Max page size = 500 (защита от слишком больших запросов)

## 9. Security Considerations

- **Authorization**: 
  - Все операторы могут просматривать сток (ROLE_USER достаточно)
  - Специальных ролей не требуется

- **Validation**: 
  - Проверка page >= 0, size > 0 && size <= 500
  - Проверка asOfDate <= now (нельзя смотреть в будущее)
  - Валидация форматов дат (ISO-8601)

- **Audit**: 
  - НЕ логировать просмотр стока (слишком частая операция)
  - Логировать создание движений (PalletMovement) через AuditLog

## 10. Rollback Plan

- **Database Rollback**: 
  - Откат миграции индексов: DROP INDEX (safe, не ломает данные)
  - Откат миграции истории: DELETE FROM pallet_movements WHERE created_by = 'MIGRATION'

- **Code Rollback**: 
  - Удалить новые endpoints (обратная совместимость OK, новые клиенты просто не будут работать)
  - Откатить изменения в ReceivingWorkflowService, PlacementWorkflowService (проверить, что старая логика работает)

- **Data Migration**: 
  - Если нужно откатить мигрированную историю → использовать DELETE с фильтром по created_by

## 11. Questions & Decisions Log

### Open Questions
- [x] Нужен ли MovementType enum или достаточно String? → **Решение**: Создадим enum для type safety
- [x] Как хранить quantity в PalletMovement (для будущих частичных отборов)? → **Решение**: Добавим поле quantity в PalletMovement (nullable, для будущего)
- [ ] Нужна ли поддержка WebSocket для real-time обновлений? → **Решение**: Пока НЕТ, только manual refresh

### Decisions Made

- **Decision**: Использовать существующую таблицу `pallet_movements` без изменений
  - **Rationale**: Схема уже идеально подходит, не нужно дублировать как StockMovement
  - **Alternatives Considered**: Создать новую таблицу stock_movements (отклонено - дублирование)
  - **Date**: 2026-01-11

- **Decision**: Point-in-time view реализуем через фильтрацию PalletMovement по timestamp
  - **Rationale**: Простая и эффективная реализация без snapshot'ов
  - **Alternatives Considered**: Snapshot-based (слишком сложно и затратно по памяти)
  - **Date**: 2026-01-11

- **Decision**: Мигрируем историю из Task/Scan в PalletMovement
  - **Rationale**: Нужны реалистичные данные для тестирования и демо
  - **Alternatives Considered**: Начать с нуля (отклонено - нет истории для теста)
  - **Date**: 2026-01-11

- **Decision**: Паллеты с qty=0 скрываем из текущего стока, но показываем в историческом
  - **Rationale**: Пустые паллеты не интересны для инвентаризации, но важны для аудита
  - **Alternatives Considered**: Показывать всегда (отклонено - засоряет UI)
  - **Date**: 2026-01-11

- **Decision**: Добавим поле `quantity` в PalletMovement (nullable)
  - **Rationale**: Подготовка к будущим частичным отборам и перемещениям
  - **Alternatives Considered**: Не добавлять сейчас (отклонено - придется делать миграцию позже)
  - **Date**: 2026-01-11

## 12. Success Metrics

- Время загрузки списка стока (без фильтров): < 1 сек для 1000 паллет
- Время загрузки истории паллеты: < 200 мс для 50 движений
- Точность point-in-time view: 100% совпадение с реальным состоянием на дату
- Использование индексов: 100% запросов должны использовать индексы (проверить через EXPLAIN)
- Удобство UI: менеджеры могут найти нужную паллету за < 10 секунд

## 13. Timeline Estimate

- **Planning**: 2 hours (DONE)
- **Phase 1 (Data Model)**: 2 hours (индексы, enum)
- **Phase 2 (Service Layer)**: 8 hours (StockService, StockMovementService, tests)
- **Phase 3 (API Layer)**: 6 hours (Controller, Mapper, DTOs, tests)
- **Phase 4 (Integration)**: 4 hours (Workflow integration, миграция истории)
- **Phase 5 (Desktop Client)**: 8 hours (JavaFX UI, REST integration)
- **Phase 6 (Documentation)**: 2 hours (API docs, user guide)
- **Testing & Bugfixes**: 4 hours
- **Total**: ~36 hours (4-5 дней)

## 14. References

- Existing code:
  - `core-api/src/main/java/com/wmsdipl/core/domain/Pallet.java` - основная entity
  - `core-api/src/main/java/com/wmsdipl/core/domain/PalletMovement.java` - история движений
  - `core-api/src/main/java/com/wmsdipl/core/service/workflow/ReceivingWorkflowService.java` - интеграция для RECEIVE
  - `core-api/src/main/java/com/wmsdipl/core/service/workflow/PlacementWorkflowService.java` - интеграция для PLACE

- Similar features:
  - TaskController - пример REST API с фильтрацией
  - ReceiptService - пример работы с пагинацией

- External resources:
  - Spring Data JPA Specifications: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications
  - JavaFX TableView: https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/TableView.html
