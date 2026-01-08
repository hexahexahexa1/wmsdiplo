# WMS Receiving Workflow - Development Plan

> **Документ для GPT 5.1 Codex**  
> Проект: WMSDIPL  
> Дата создания: 2026-01-06  
> Статус: PLAN / NOT IMPLEMENTED

---

## Оглавление

1. [Обзор процесса](#1-обзор-процесса)
2. [Текущее состояние](#2-текущее-состояние)
3. [Целевой workflow](#3-целевой-workflow)
4. [Стратегии размещения](#4-стратегии-размещения)
5. [Топология склада](#5-топология-склада)
6. [Управление паллетами](#6-управление-паллетами)
7. [Миграции БД](#7-миграции-бд)
8. [JPA Entities](#8-jpa-entities)
9. [Сервисы и API](#9-сервисы-и-api)
10. [Desktop Client UI](#10-desktop-client-ui)
11. [Порядок реализации](#11-порядок-реализации)

---

## 1. Обзор процесса

### Целевой бизнес-процесс приёмки:

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   DRAFT      │────▶│  CONFIRMED   │────▶│ IN_PROGRESS  │────▶│   ACCEPTED   │
│  (импорт)    │     │ (к приёмке)  │     │  (приёмка)   │     │  (принят)    │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
                                                                      │
                    ┌──────────────┐     ┌──────────────┐            │
                    │   STOCKED    │◀────│  PLACING     │◀───────────┘
                    │ (на складе)  │     │(размещение)  │
                    └──────────────┘     └──────────────┘
```

### Детальные шаги:

| Шаг | Статус | Действие | Результат |
|-----|--------|----------|-----------|
| 1 | DRAFT | Импорт документа через API | Создан receipt + lines |
| 2 | CONFIRMED | Оператор подтверждает к приёмке | Генерируются задания RECEIVING |
| 3 | IN_PROGRESS | Выполнение заданий приёмки | Сканы, привязка к паллетам |
| 4 | ACCEPTED | Все задания закрыты, расхождения resolved | Готов к размещению |
| 5 | PLACING | Генерация заданий PLACEMENT | Назначение locations по стратегии |
| 6 | STOCKED | Все паллеты размещены | Документ приходован |

---

## 2. Текущее состояние

### Существующие компоненты:

```
core-api/
├── domain/
│   ├── Receipt.java           ✅ Реализовано
│   ├── ReceiptLine.java       ✅ Реализовано
│   └── ReceiptStatus.java     ⚠️ Только DRAFT, CONFIRMED, ACCEPTED
├── repository/
│   └── ReceiptRepository.java ✅ Реализовано
├── service/
│   └── ReceiptService.java    ✅ CRUD + confirm/accept
└── api/
    ├── ReceiptController.java ✅ REST endpoints
    └── ImportController.java  ✅ Import endpoint
```

### Существующая схема БД (V1__init.sql):

| Таблица | Статус | Комментарий |
|---------|--------|-------------|
| receipts | ✅ | Есть entity |
| receipt_lines | ✅ | Есть entity |
| skus | ⚠️ | Нет entity |
| packagings | ⚠️ | Нет entity |
| tasks | ⚠️ | Таблица есть, entity нет |
| scans | ⚠️ | Таблица есть, entity нет |
| discrepancies | ⚠️ | Таблица есть, entity нет |
| import_log | ⚠️ | Таблица есть, entity нет |
| status_history | ⚠️ | Таблица есть, entity нет |

### Отсутствующие компоненты:

- [ ] Топология склада (zones, locations, bins)
- [ ] Паллеты (pallets)
- [ ] Пользователи/сотрудники (users)
- [ ] Стратегии размещения
- [ ] TaskService, ScanService, DiscrepancyService
- [ ] Inventory/Stock tracking

---

## 3. Целевой workflow

### Статусы Receipt (расширенные):

```java
public enum ReceiptStatus {
    DRAFT,              // Импортирован, черновик
    CONFIRMED,          // Подтверждён к приёмке
    IN_PROGRESS,        // Приёмка выполняется
    PENDING_RESOLUTION, // Есть нерешённые расхождения
    ACCEPTED,           // Приёмка завершена
    PLACING,            // Размещение в процессе
    STOCKED,            // Размещён на складе (финал)
    CANCELLED           // Отменён
}
```

### Статусы Task:

```java
public enum TaskStatus {
    NEW,           // Создано
    ASSIGNED,      // Назначено исполнителю
    IN_PROGRESS,   // В работе
    COMPLETED,     // Выполнено
    CANCELLED      // Отменено
}

public enum TaskType {
    RECEIVING,     // Приёмка товара
    PLACEMENT,     // Размещение на адрес
    REPLENISHMENT, // Пополнение (будущее)
    PICKING        // Отбор (будущее)
}
```

### Переходы статусов Receipt:

```
DRAFT ──[confirm()]──▶ CONFIRMED
CONFIRMED ──[startReceiving()]──▶ IN_PROGRESS
IN_PROGRESS ──[hasUnresolvedDiscrepancies()]──▶ PENDING_RESOLUTION
PENDING_RESOLUTION ──[resolveAll()]──▶ IN_PROGRESS
IN_PROGRESS ──[allTasksCompleted()]──▶ ACCEPTED
ACCEPTED ──[startPlacement()]──▶ PLACING
PLACING ──[allPalletsPlaced()]──▶ STOCKED

* ──[cancel()]──▶ CANCELLED (из любого статуса кроме STOCKED)
```

---

## 4. Стратегии размещения

### Реализуемые стратегии:

#### 4.1 CLOSEST_AVAILABLE (Ближайшее свободное)

```java
/**
 * Размещение на ближайшую свободную ячейку от точки приёмки.
 * 
 * Логика:
 * 1. Определить допустимые зоны по атрибутам SKU
 * 2. Найти все пустые locations в этих зонах
 * 3. Отсортировать по расстоянию от receiving_dock
 * 4. Вернуть первую подходящую по габаритам/весу
 * 
 * Применение: Максимальная скорость размещения
 */
public interface ClosestAvailableStrategy {
    Location findLocation(Pallet pallet, Location currentPosition);
}
```

#### 4.2 ABC_VELOCITY (ABC по оборачиваемости)

```java
/**
 * Размещение по классу оборачиваемости товара.
 * A-class (быстрые) → золотая зона (уровень пола, близко к отгрузке)
 * B-class (средние) → средняя зона
 * C-class (медленные) → дальние/верхние ярусы
 * 
 * Логика:
 * 1. Получить velocity_class из sku_storage_config
 * 2. Определить target_zone по классу
 * 3. Внутри зоны применить CLOSEST_AVAILABLE
 * 4. При отсутствии места → spillover в соседнюю зону
 * 
 * Применение: Оптимизация отбора, много SKU
 */
public interface AbcVelocityStrategy {
    Location findLocation(Pallet pallet);
}
```

**Требуемая конфигурация:**
```sql
-- Таблица настройки хранения по SKU
CREATE TABLE sku_storage_config (
    sku_id          BIGINT PRIMARY KEY REFERENCES skus(id),
    velocity_class  VARCHAR(1) NOT NULL DEFAULT 'C',  -- A, B, C
    preferred_zone_id BIGINT REFERENCES zones(id),
    min_stock       NUMERIC(18,3) DEFAULT 0,
    max_stock       NUMERIC(18,3)
);
```

#### 4.3 CONSOLIDATION (Консолидация)

```java
/**
 * Приоритет дозаполнения существующих ячеек с тем же SKU.
 * 
 * Логика:
 * 1. Найти locations с тем же SKU и свободным местом
 * 2. Отсортировать по % заполнения (desc) - сначала почти полные
 * 3. Если нет - создать новую позицию (fallback на ABC/CLOSEST)
 * 
 * Применение: Экономия места, меньше фрагментации
 */
public interface ConsolidationStrategy {
    Location findLocation(Pallet pallet);
}
```

#### 4.4 FIFO_DIRECTED (FIFO-ориентированное)

```java
/**
 * Размещение с учётом сроков годности/лотов.
 * Новые партии размещаются "за" старыми.
 * 
 * Логика:
 * 1. Получить lot_date/expiry_date паллета
 * 2. Найти locations с тем же SKU и более старыми лотами
 * 3. Разместить в соседнюю ячейку (для flow racks) или отдельный lane
 * 4. Если новый лот - обычное размещение
 * 
 * Применение: Продукты питания, фарма, химия
 */
public interface FifoDirectedStrategy {
    Location findLocation(Pallet pallet);
}
```

### Конфигурация стратегий:

```sql
-- Правила размещения (приоритетные)
CREATE TABLE putaway_rules (
    id              BIGSERIAL PRIMARY KEY,
    priority        INTEGER NOT NULL DEFAULT 100,
    name            VARCHAR(128) NOT NULL,
    strategy_type   VARCHAR(32) NOT NULL,  -- CLOSEST, ABC, CONSOLIDATION, FIFO
    
    -- Фильтры применимости
    zone_id         BIGINT REFERENCES zones(id),      -- null = все зоны
    sku_category    VARCHAR(64),                       -- null = все категории
    velocity_class  VARCHAR(1),                        -- null = все классы
    
    -- Параметры стратегии
    params          JSONB,  -- {"spillover_zones": [2,3], "consolidate_threshold": 0.8}
    
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now()
);

-- Пример правил:
INSERT INTO putaway_rules (priority, name, strategy_type, velocity_class) VALUES
(10, 'Fast movers to golden zone', 'ABC', 'A'),
(20, 'Medium movers consolidate', 'CONSOLIDATION', 'B'),
(30, 'Slow movers any available', 'CLOSEST', 'C'),
(100, 'Default fallback', 'CLOSEST', NULL);
```

### Интерфейс стратегии:

```java
// core-api/src/main/java/com/wmsdipl/core/service/putaway/PutawayStrategy.java

public interface PutawayStrategy {
    
    /**
     * Найти location для размещения паллета
     * @param pallet паллет для размещения
     * @param context контекст (текущая позиция, receipt и т.д.)
     * @return Optional<Location> - пустой если места нет
     */
    Optional<Location> findLocation(Pallet pallet, PutawayContext context);
    
    /**
     * Тип стратегии для matching с putaway_rules
     */
    String getStrategyType();
}

// Сервис выбора стратегии
public interface PutawayService {
    
    /**
     * Выбрать стратегию и найти location
     */
    Location determineLocation(Pallet pallet);
    
    /**
     * Сгенерировать задания PLACEMENT для receipt
     */
    List<Task> generatePlacementTasks(Receipt receipt);
}
```

---

## 5. Топология склада

### Иерархия:

```
Warehouse (implicit, single-tenant)
└── Zone (зона хранения)
    └── Aisle (проход)
        └── Bay (секция стеллажа)
            └── Level (ярус)
                └── Bin (ячейка) = Location
```

### Требования к UI настройки:

> **Клиент должен иметь возможность самостоятельно:**
> - Создавать/редактировать зоны
> - Добавлять ряды (aisle), секции (bay), уровни (level)
> - Генерировать ячейки пакетно или поштучно
> - Задавать габариты и ограничения для ячеек
> - Деактивировать ячейки (ремонт, блокировка)

### Структура Location code:

```
Формат: {ZONE}-{AISLE}{BAY}-{LEVEL}
Пример: A-01B-03 = Зона A, Проход 01, Секция B, Ярус 03

Альтернативный формат (настраиваемый):
{ZONE}{AISLE}{BAY}{LEVEL} = A01B03
```

### UI Desktop Client - экран "Топология":

```
┌─────────────────────────────────────────────────────────────┐
│ Топология склада                                            │
├─────────────────────────────────────────────────────────────┤
│ [+ Зона] [+ Проход] [+ Секция] [Генерация ячеек...]        │
├──────────────┬──────────────────────────────────────────────┤
│ Зоны         │ Ячейки зоны: ЗОНА-А (Основное хранение)     │
│ ─────────────│ ────────────────────────────────────────────│
│ ▶ ЗОНА-А     │ Код      │ Статус   │ Габариты   │ Вес     │
│   ЗОНА-Б     │ A-01A-01 │ Свободна │ 120x100x150│ 1500 кг │
│   ЗОНА-ХОЛОД │ A-01A-02 │ Занята   │ 120x100x150│ 1500 кг │
│   КАРАНТИН   │ A-01A-03 │ Блок.    │ 120x100x150│ 1500 кг │
│              │ A-01B-01 │ Свободна │ 120x100x150│ 1500 кг │
│              │ ...      │          │            │         │
└──────────────┴──────────────────────────────────────────────┘
```

---

## 6. Управление паллетами

### Требования:

> **Клиент должен иметь возможность:**
> - Генерировать пулы кодов паллет (SSCC или внутренние)
> - Печатать этикетки паллет
> - Вводить коды вручную при приёмке
> - Видеть историю перемещений паллета

### Статусы паллета:

```java
public enum PalletStatus {
    EMPTY,       // Пустой (в пуле)
    RECEIVING,   // На приёмке (привязывается к receipt_line)
    RECEIVED,    // Принят, ожидает размещения
    IN_TRANSIT,  // Перемещается (задание в работе)
    PLACED,      // Размещён на адресе
    PICKING,     // На отборе (будущее)
    SHIPPED      // Отгружен (будущее)
}
```

### UI Desktop Client - экран "Паллеты":

```
┌─────────────────────────────────────────────────────────────┐
│ Управление паллетами                                        │
├─────────────────────────────────────────────────────────────┤
│ [Генерировать коды...] [Импорт SSCC...] [Печать этикеток]  │
├─────────────────────────────────────────────────────────────┤
│ Фильтр: [Все статусы ▼] [____________] [Поиск]             │
├─────────────────────────────────────────────────────────────┤
│ Код паллета      │ Статус    │ Location   │ SKU            │
│ ─────────────────│───────────│────────────│────────────────│
│ 00123456789012345│ PLACED    │ A-01A-02   │ SKU-001 (150шт)│
│ 00123456789012346│ RECEIVING │ DOCK-01    │ -              │
│ 00123456789012347│ EMPTY     │ -          │ -              │
│ SSCC-12345678    │ PLACED    │ B-02C-01   │ SKU-042 (80шт) │
└─────────────────────────────────────────────────────────────┘
```

### Генерация кодов паллет:

```java
public interface PalletCodeGenerator {
    
    /**
     * Генерация пула внутренних кодов
     * @param prefix префикс (опционально)
     * @param count количество
     * @return список сгенерированных кодов
     */
    List<String> generateInternalCodes(String prefix, int count);
    
    /**
     * Генерация SSCC по GS1 стандарту
     * @param companyPrefix GS1 prefix компании
     * @param count количество
     */
    List<String> generateSSCC(String companyPrefix, int count);
    
    /**
     * Валидация кода (уникальность, формат)
     */
    boolean validateCode(String code);
}
```

---

## 7. Миграции БД

### V2__warehouse_topology.sql

```sql
-- Зоны склада
CREATE TABLE zones (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(32) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL,
    zone_type       VARCHAR(32) NOT NULL DEFAULT 'STORAGE',  -- STORAGE, RECEIVING, SHIPPING, COLD, HAZMAT
    priority_rank   INTEGER DEFAULT 100,
    description     VARCHAR(512),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now()
);

-- Ячейки хранения (locations)
CREATE TABLE locations (
    id              BIGSERIAL PRIMARY KEY,
    zone_id         BIGINT NOT NULL REFERENCES zones(id),
    code            VARCHAR(32) NOT NULL UNIQUE,
    
    -- Позиционирование
    aisle           VARCHAR(8),
    bay             VARCHAR(8),
    level           VARCHAR(8),
    
    -- Координаты (для расчёта расстояний)
    x_coord         NUMERIC(10,2),
    y_coord         NUMERIC(10,2),
    z_coord         NUMERIC(10,2),  -- высота яруса
    
    -- Ограничения
    max_weight_kg   NUMERIC(10,2),
    max_height_cm   NUMERIC(10,2),
    max_width_cm    NUMERIC(10,2),
    max_depth_cm    NUMERIC(10,2),
    max_pallets     INTEGER DEFAULT 1,
    
    -- Статус
    status          VARCHAR(32) DEFAULT 'AVAILABLE',  -- AVAILABLE, OCCUPIED, BLOCKED, RESERVED
    
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_locations_zone ON locations(zone_id);
CREATE INDEX idx_locations_status ON locations(status);
CREATE INDEX idx_locations_code ON locations(code);

-- Связь SKU с настройками хранения
CREATE TABLE sku_storage_config (
    id              BIGSERIAL PRIMARY KEY,
    sku_id          BIGINT NOT NULL REFERENCES skus(id),
    velocity_class  VARCHAR(1) DEFAULT 'C',  -- A, B, C
    preferred_zone_id BIGINT REFERENCES zones(id),
    hazmat_class    VARCHAR(16),
    temp_range      VARCHAR(32),  -- AMBIENT, COLD, FROZEN
    min_stock       NUMERIC(18,3) DEFAULT 0,
    max_stock       NUMERIC(18,3),
    CONSTRAINT sku_storage_config_sku_unique UNIQUE (sku_id)
);

-- Правила размещения
CREATE TABLE putaway_rules (
    id              BIGSERIAL PRIMARY KEY,
    priority        INTEGER NOT NULL DEFAULT 100,
    name            VARCHAR(128) NOT NULL,
    strategy_type   VARCHAR(32) NOT NULL,  -- CLOSEST, ABC, CONSOLIDATION, FIFO
    zone_id         BIGINT REFERENCES zones(id),
    sku_category    VARCHAR(64),
    velocity_class  VARCHAR(1),
    params          JSONB,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now()
);
```

### V3__pallets.sql

```sql
-- Паллеты
CREATE TABLE pallets (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,  -- SSCC или внутренний код
    code_type       VARCHAR(16) DEFAULT 'INTERNAL',  -- INTERNAL, SSCC
    
    -- Текущее состояние
    status          VARCHAR(32) NOT NULL DEFAULT 'EMPTY',
    location_id     BIGINT REFERENCES locations(id),
    
    -- Содержимое (если не пустой)
    sku_id          BIGINT REFERENCES skus(id),
    lot_number      VARCHAR(64),
    expiry_date     DATE,
    quantity        NUMERIC(18,3),
    uom             VARCHAR(32),
    
    -- Связь с документом
    receipt_id      BIGINT REFERENCES receipts(id),
    receipt_line_id BIGINT REFERENCES receipt_lines(id),
    
    -- Габариты/вес
    weight_kg       NUMERIC(10,2),
    height_cm       NUMERIC(10,2),
    
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_pallets_status ON pallets(status);
CREATE INDEX idx_pallets_location ON pallets(location_id);
CREATE INDEX idx_pallets_sku ON pallets(sku_id);
CREATE INDEX idx_pallets_receipt ON pallets(receipt_id);

-- Пул кодов паллет (сгенерированные, но не использованные)
CREATE TABLE pallet_code_pool (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    code_type       VARCHAR(16) DEFAULT 'INTERNAL',
    is_used         BOOLEAN DEFAULT FALSE,
    generated_at    TIMESTAMP DEFAULT now(),
    used_at         TIMESTAMP
);

-- История перемещений паллета
CREATE TABLE pallet_movements (
    id              BIGSERIAL PRIMARY KEY,
    pallet_id       BIGINT NOT NULL REFERENCES pallets(id),
    from_location_id BIGINT REFERENCES locations(id),
    to_location_id  BIGINT REFERENCES locations(id),
    movement_type   VARCHAR(32),  -- RECEIVING, PLACEMENT, REPLENISHMENT, PICKING
    task_id         BIGINT REFERENCES tasks(id),
    moved_by        VARCHAR(128),
    moved_at        TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_pallet_movements_pallet ON pallet_movements(pallet_id);
```

### V4__users.sql

```sql
-- Пользователи системы
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(256) NOT NULL,
    full_name       VARCHAR(255),
    email           VARCHAR(255),
    role            VARCHAR(32) NOT NULL DEFAULT 'OPERATOR',  -- ADMIN, SUPERVISOR, OPERATOR
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

-- Начальный admin пользователь (пароль: admin)
INSERT INTO users (username, password_hash, full_name, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrYV4P9.M2OQ7z6O5VqfHV1./', 'Administrator', 'ADMIN');
```

### V5__extend_tasks.sql

```sql
-- Расширение таблицы tasks
ALTER TABLE tasks ADD COLUMN task_type VARCHAR(32) DEFAULT 'RECEIVING';
ALTER TABLE tasks ADD COLUMN pallet_id BIGINT REFERENCES pallets(id);
ALTER TABLE tasks ADD COLUMN source_location_id BIGINT REFERENCES locations(id);
ALTER TABLE tasks ADD COLUMN target_location_id BIGINT REFERENCES locations(id);
ALTER TABLE tasks ADD COLUMN assigned_by VARCHAR(128);
ALTER TABLE tasks ADD COLUMN started_at TIMESTAMP;
ALTER TABLE tasks ADD COLUMN priority INTEGER DEFAULT 100;

CREATE INDEX idx_tasks_type ON tasks(task_type);
CREATE INDEX idx_tasks_pallet ON tasks(pallet_id);
CREATE INDEX idx_tasks_assignee ON tasks(assignee);
```

---

## 8. JPA Entities

### Новые entities для создания:

```
core-api/src/main/java/com/wmsdipl/core/domain/
├── Zone.java
├── Location.java
├── LocationStatus.java (enum)
├── Pallet.java
├── PalletStatus.java (enum)
├── PalletMovement.java
├── Task.java
├── TaskStatus.java (enum)
├── TaskType.java (enum)
├── Scan.java
├── Discrepancy.java
├── Sku.java
├── SkuStorageConfig.java
├── PutawayRule.java
├── User.java
├── UserRole.java (enum)
└── PalletCodePool.java
```

### Пример: Zone.java

```java
package com.wmsdipl.core.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "zones")
public class Zone {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 32)
    private String code;
    
    @Column(nullable = false, length = 128)
    private String name;
    
    @Column(name = "zone_type", length = 32)
    private String zoneType = "STORAGE";
    
    @Column(name = "priority_rank")
    private Integer priorityRank = 100;
    
    private String description;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @OneToMany(mappedBy = "zone")
    private List<Location> locations;
    
    // getters, setters, constructors
}
```

### Пример: Location.java

```java
package com.wmsdipl.core.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "locations")
public class Location {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;
    
    @Column(nullable = false, unique = true, length = 32)
    private String code;
    
    private String aisle;
    private String bay;
    private String level;
    
    @Column(name = "x_coord")
    private BigDecimal xCoord;
    
    @Column(name = "y_coord")
    private BigDecimal yCoord;
    
    @Column(name = "z_coord")
    private BigDecimal zCoord;
    
    @Column(name = "max_weight_kg")
    private BigDecimal maxWeightKg;
    
    @Column(name = "max_height_cm")
    private BigDecimal maxHeightCm;
    
    @Column(name = "max_pallets")
    private Integer maxPallets = 1;
    
    @Enumerated(EnumType.STRING)
    private LocationStatus status = LocationStatus.AVAILABLE;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at") 
    private Instant updatedAt;
    
    // getters, setters
}
```

### Пример: Pallet.java

```java
package com.wmsdipl.core.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pallets")
public class Pallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    
    @Column(name = "code_type", length = 16)
    private String codeType = "INTERNAL";
    
    @Enumerated(EnumType.STRING)
    private PalletStatus status = PalletStatus.EMPTY;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id")
    private Sku sku;
    
    @Column(name = "lot_number")
    private String lotNumber;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    private BigDecimal quantity;
    private String uom;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id")
    private Receipt receipt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_line_id")
    private ReceiptLine receiptLine;
    
    @Column(name = "weight_kg")
    private BigDecimal weightKg;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    // getters, setters
}
```

---

## 9. Сервисы и API

### Новые сервисы:

| Сервис | Ответственность |
|--------|-----------------|
| `ZoneService` | CRUD зон, валидация |
| `LocationService` | CRUD ячеек, генерация пакетная, поиск свободных |
| `PalletService` | CRUD паллет, генерация кодов, перемещение |
| `TaskService` | CRUD задач, назначение, статус-машина |
| `ScanService` | Запись сканов, валидация SSCC |
| `DiscrepancyService` | Создание/разрешение расхождений |
| `PutawayService` | Выбор стратегии, поиск location |
| `ReceivingWorkflowService` | Оркестрация всего процесса |
| `UserService` | Аутентификация, CRUD пользователей |

### Новые API endpoints:

```
# Топология
GET    /api/zones                    # Список зон
POST   /api/zones                    # Создать зону
PUT    /api/zones/{id}               # Обновить зону
DELETE /api/zones/{id}               # Удалить зону

GET    /api/locations                # Список ячеек (с фильтрами)
POST   /api/locations                # Создать ячейку
POST   /api/locations/generate       # Пакетная генерация
PUT    /api/locations/{id}           # Обновить
PUT    /api/locations/{id}/status    # Изменить статус (block/unblock)

# Паллеты  
GET    /api/pallets                  # Список (с фильтрами)
POST   /api/pallets                  # Создать паллет
POST   /api/pallets/generate         # Генерация пула кодов
GET    /api/pallets/{id}/movements   # История перемещений
PUT    /api/pallets/{id}/location    # Переместить (ручное)

# Задачи
GET    /api/tasks                    # Список (с фильтрами по receipt, assignee, status)
GET    /api/tasks/{id}               # Детали задачи
POST   /api/tasks/{id}/assign        # Назначить исполнителя
POST   /api/tasks/{id}/start         # Начать выполнение
POST   /api/tasks/{id}/complete      # Завершить
POST   /api/tasks/{id}/cancel        # Отменить

# Расширение Receipt workflow
POST   /api/receipts/{id}/start-receiving    # CONFIRMED → IN_PROGRESS + создать tasks
POST   /api/receipts/{id}/start-placement    # ACCEPTED → PLACING + создать placement tasks
POST   /api/receipts/{id}/complete-placement # PLACING → STOCKED (авто при закрытии всех tasks)

# Расхождения
GET    /api/discrepancies            # Список нерешённых
POST   /api/discrepancies/{id}/resolve   # Разрешить

# Справочники
GET    /api/skus                     # SKU каталог
PUT    /api/skus/{id}/storage-config # Настройка хранения SKU

# Правила размещения
GET    /api/putaway-rules
POST   /api/putaway-rules
PUT    /api/putaway-rules/{id}
DELETE /api/putaway-rules/{id}
```

---

## 10. Desktop Client UI

### Новые экраны:

| Экран | Навигация | Функционал |
|-------|-----------|------------|
| Топология | Топология | Управление зонами, ячейками |
| Паллеты | Паллеты | Управление кодами паллет |
| Задачи | Задания | Список задач, назначение, мониторинг |
| Пользователи | Настройки → Пользователи | CRUD пользователей |
| Правила размещения | Настройки → Размещение | Настройка стратегий |

### Изменения в существующих экранах:

**Приходы (Receipts):**
- Добавить столбец "Тип задач" (RECEIVING/PLACEMENT)
- Кнопка "Начать приёмку" (confirm → start-receiving)
- Кнопка "Начать размещение" (accept → start-placement)
- Просмотр связанных задач
- Просмотр расхождений

---

## 11. Порядок реализации

### Фаза 1: Инфраструктура (1-2 дня)

- [ ] V2__warehouse_topology.sql - миграция
- [ ] Zone entity + ZoneRepository + ZoneService
- [ ] Location entity + LocationRepository + LocationService
- [ ] LocationController (CRUD + generate)
- [ ] ZoneController (CRUD)
- [ ] Обновить ReceiptStatus enum

### Фаза 2: Паллеты (1-2 дня)

- [ ] V3__pallets.sql - миграция
- [ ] Pallet entity + PalletRepository + PalletService
- [ ] PalletCodeGenerator (internal + SSCC)
- [ ] PalletMovement entity
- [ ] PalletController (CRUD + generate + movements)

### Фаза 3: Задачи (2-3 дня)

- [ ] V5__extend_tasks.sql - миграция
- [ ] Task entity + TaskRepository + TaskService
- [ ] TaskStatus, TaskType enums
- [ ] Scan entity + ScanService
- [ ] Discrepancy entity + DiscrepancyService
- [ ] TaskController (CRUD + assign/start/complete)
- [ ] DiscrepancyController

### Фаза 4: Workflow (2-3 дня)

- [ ] ReceivingWorkflowService
  - [ ] startReceiving() - создание RECEIVING tasks
  - [ ] recordScan() - валидация, создание discrepancies
  - [ ] completeReceiving() - проверка tasks + discrepancies
- [ ] Интеграция с ReceiptService (новые переходы статусов)

### Фаза 5: Стратегии размещения (2-3 дня)

- [ ] PutawayStrategy interface
- [ ] ClosestAvailableStrategy
- [ ] AbcVelocityStrategy
- [ ] ConsolidationStrategy
- [ ] FifoDirectedStrategy
- [ ] PutawayService (выбор стратегии, генерация PLACEMENT tasks)
- [ ] PutawayRuleController

### Фаза 6: Desktop Client UI (3-4 дня)

- [ ] Экран "Топология" (зоны, ячейки, генерация)
- [ ] Экран "Паллеты" (список, генерация кодов)
- [ ] Экран "Задачи" (список, назначение)
- [ ] Обновление экрана "Приходы" (новые кнопки workflow)
- [ ] Экран "Настройки → Размещение" (правила)

### Фаза 7: Пользователи (1-2 дня)

- [ ] V4__users.sql - миграция
- [ ] User entity + UserService
- [ ] Интеграция с Spring Security (замена hardcoded admin)
- [ ] UserController (CRUD)
- [ ] Обновление login в Desktop Client

---

## Примечания для Codex

### Контекст проекта:

- **Язык**: Java 17
- **Framework**: Spring Boot 3.2.x
- **ORM**: Hibernate 6.4.x / Spring Data JPA
- **БД**: PostgreSQL 16
- **Миграции**: Flyway
- **Desktop**: JavaFX 21
- **Build**: Gradle

### Структура модулей:

```
WMSDIPL/
├── core-api/          # Spring Boot REST API
├── desktop-client/    # JavaFX клиент
├── import-service/    # Импорт из XML (не трогать)
└── docs/              # Документация
```

### Conventions:

- Entities в `domain/`
- Repositories в `repository/`
- Services в `service/`
- Controllers в `api/`
- DTOs в `api/dto/`
- Enums в `domain/` рядом с entity

### При реализации:

1. Создавать миграции инкрементально (V2, V3, ...)
2. Покрывать сервисы unit-тестами
3. Использовать DTO для API (не возвращать entities напрямую)
4. Логировать переходы статусов в status_history
5. Валидировать входные данные в контроллерах (@Valid)
