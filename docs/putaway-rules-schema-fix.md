# Исправление схемы таблицы putaway_rules

## Дата: 2026-01-12

## Проблема
В настройках desktop-client не отображались правила размещения (схемы putaway).
Ошибка: "Внутренняя ошибка сервера при выполнении операции '/api/putaway-rules'"

## Причина
Таблица `putaway_rules` в PostgreSQL имела устаревшую схему, несовместимую с Entity классом:

### Было в БД:
```sql
- id
- sku_id              -- старая колонка
- zone_id
- strategy            -- неправильное имя
- priority
- is_active
- created_at
```

### Должно быть (из миграции V6):
```sql
- id
- priority
- name                -- отсутствовала!
- strategy_type       -- правильное имя
- zone_id
- sku_category        -- отсутствовала!
- velocity_class      -- отсутствовала!
- params              -- отсутствовала! (JSONB)
- is_active
- created_at
```

## Примененные изменения

### 1. Код: PutawayRuleController.java
Добавлена аннотация `@Transactional(readOnly = true)` к методам GET для 
корректной работы с LAZY загрузкой связанной сущности `Zone`:

```java
@GetMapping
@Transactional(readOnly = true)
public List<PutawayRuleDto> list() { ... }

@GetMapping("/{id}")
@Transactional(readOnly = true)
public ResponseEntity<PutawayRuleDto> get(@PathVariable Long id) { ... }
```

### 2. База данных: Пересоздание таблицы
```sql
DROP TABLE IF EXISTS putaway_rules CASCADE;

CREATE TABLE putaway_rules (
    id              BIGSERIAL PRIMARY KEY,
    priority        INTEGER NOT NULL DEFAULT 100,
    name            VARCHAR(128) NOT NULL,
    strategy_type   VARCHAR(32) NOT NULL,
    zone_id         BIGINT REFERENCES zones(id),
    sku_category    VARCHAR(64),
    velocity_class  VARCHAR(1),
    params          JSONB,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_putaway_rules_active ON putaway_rules(is_active);
CREATE INDEX idx_putaway_rules_priority ON putaway_rules(priority);

INSERT INTO putaway_rules (priority, name, strategy_type, is_active)
VALUES (100, 'Default fallback', 'CLOSEST', TRUE);
```

## Проверка работоспособности

### Через API:
```bash
curl -X GET http://localhost:8080/api/putaway-rules \
  -u admin:admin \
  -H "Accept: application/json"
```

**Ожидаемый результат:**
```json
[{
  "id": 1,
  "priority": 100,
  "name": "Default fallback",
  "strategyType": "CLOSEST",
  "zoneId": null,
  "skuCategory": null,
  "velocityClass": null,
  "params": null,
  "active": true
}]
```

### Через Desktop Client:
1. Запустить: `gradle :desktop-client:run`
2. Войти: `admin` / `admin`
3. Перейти в раздел **"Настройки"**
4. Секция **"Правила размещения"**
5. Ожидаемый результат: 
   - ✅ Таблица отображается с одной записью "Default fallback"
   - ✅ Колонки: priority, name, strategy, zoneId, velocity, active
   - ✅ Кнопка "Обновить правила" работает

## Доступные стратегии размещения

Согласно коду (package `com.wmsdipl.core.service.putaway`):

- **CLOSEST** - ближайшее доступное место
- **FIFO_DIRECTED** - FIFO с направленным размещением
- **FEFO** - First Expired, First Out
- **ABC_ZONE** - размещение по ABC-классификации
- **CONSOLIDATION** - консолидация товаров
- **VELOCITY** - размещение по скорости оборота

## Примечания

- Таблица была пересоздана вручную (без миграций)
- При пересоздании БД миграция V6 создаст правильную схему автоматически
- Вставлено правило по умолчанию: "Default fallback" со стратегией CLOSEST
- Все старые данные были удалены (таблица была пустая)

## Связанные изменения

- Добавлен импорт `org.springframework.transaction.annotation.Transactional` в PutawayRuleController
- Улучшена обработка LAZY загрузки для связанной сущности Zone
