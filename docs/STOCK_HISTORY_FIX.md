# Исправление ошибки "История перемещений паллеты"

## Проблема
При двойном клике на паллету в экране "Остатки на складе" возникает ошибка:
```
java.lang.RuntimeException: java.io.IOException: Внутренняя ошибка сервера при выполнении операции /api/st...
```

## Причина №1: LazyInitializationException (ИСПРАВЛЕНО)
Проблема была в ленивой загрузке (lazy loading) связанных сущностей в `PalletMovement`.

**Решение**: Добавлен `JOIN FETCH` в запрос репозитория для явной загрузки всех связей.

**Файл**: `core-api/src/main/java/com/wmsdipl/core/repository/PalletMovementRepository.java`

Изменен метод `findByPalletOrderByMovedAtDesc` - теперь использует JOIN FETCH для загрузки `pallet`, `fromLocation`, `toLocation`.

## Причина №2: Отсутствие данных в таблице pallet_movements
Если ошибка сохраняется, возможно в базе данных нет записей истории перемещений.

### Диагностика

1. **Остановите desktop-client** (если запущен)

2. **Остановите core-api сервер**:
   ```bash
   # Найти процесс
   netstat -ano | findstr :8080
   # Остановить (замените PID на ваш)
   taskkill /PID <PID>
   ```

3. **Подключитесь к PostgreSQL** и выполните проверочные запросы:
   ```bash
   docker exec -it wmsdipl-postgres-1 psql -U wmsdipl -d wmsdipl
   ```

4. **Выполните диагностику**:
   ```sql
   -- Проверить существование таблицы
   SELECT EXISTS (
       SELECT FROM information_schema.tables 
       WHERE table_name = 'pallet_movements'
   );

   -- Посчитать записи
   SELECT COUNT(*) FROM pallet_movements;

   -- Посмотреть существующие движения
   SELECT pm.id, pm.movement_type, p.code, pm.moved_at
   FROM pallet_movements pm
   JOIN pallets p ON p.id = pm.pallet_id
   ORDER BY pm.moved_at DESC
   LIMIT 10;
   ```

### Решение: Миграция истории из tasks/scans

Если в `pallet_movements` нет данных (COUNT = 0), используйте готовый SQL-скрипт для миграции:

```bash
# Находясь в корне проекта E:\WMSDIPL
psql -h localhost -p 55432 -U wmsdipl -d wmsdipl -f check_and_fix_stock_history.sql
```

Или выполните вручную в psql:

```sql
-- Создать RECEIVE движения из scans
INSERT INTO pallet_movements (pallet_id, from_location_id, to_location_id, movement_type, task_id, moved_by, moved_at)
SELECT DISTINCT ON (p.id)
    p.id,
    NULL,
    1, -- receiving location (проверьте ID вашей зоны приемки)
    'RECEIVE',
    t.id,
    COALESCE(t.assignee, 'system'),
    COALESCE(s.created_at, p.created_at)
FROM pallets p
INNER JOIN scans s ON s.pallet_code = p.code
INNER JOIN tasks t ON t.id = s.task_id
WHERE t.task_type = 'RECEIVING'
  AND p.status IN ('RECEIVED', 'PLACED')
  AND NOT EXISTS (
      SELECT 1 FROM pallet_movements pm 
      WHERE pm.pallet_id = p.id AND pm.movement_type = 'RECEIVE'
  )
ORDER BY p.id, s.created_at ASC;

-- Создать PLACE движения из placement tasks
INSERT INTO pallet_movements (pallet_id, from_location_id, to_location_id, movement_type, task_id, moved_by, moved_at)
SELECT DISTINCT ON (p.id)
    p.id,
    1, -- from receiving
    p.location_id,
    'PLACE',
    t.id,
    COALESCE(t.assignee, 'system'),
    COALESCE(t.completed_at, t.updated_at)
FROM pallets p
INNER JOIN tasks t ON t.pallet_id = p.id
WHERE t.task_type = 'PLACEMENT'
  AND t.status = 'COMPLETED'
  AND p.location_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM pallet_movements pm 
      WHERE pm.pallet_id = p.id AND pm.movement_type = 'PLACE'
  )
ORDER BY p.id, t.completed_at ASC;
```

## Перезапуск сервисов

После выполнения миграции:

1. **Запустите core-api**:
   ```bash
   cd E:\WMSDIPL
   gradle :core-api:bootRun
   ```
   
   Дождитесь сообщения: `Started CoreApiApplication`

2. **Запустите desktop-client**:
   ```bash
   gradle :desktop-client:run
   ```

3. **Войдите в систему** (testuser / password)

4. **Перейдите в "Остатки"**

5. **Дважды кликните на паллету** с ID 2 или другую

6. **Проверьте**, что история загружается без ошибок

## Если ошибка все еще есть

Включите подробное логирование в `core-api/src/main/resources/application.yml`:

```yaml
logging:
  level:
    root: DEBUG
    com.wmsdipl.core: TRACE
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

Перезапустите core-api и посмотрите логи при открытии истории паллеты. Ищите строки с:
- `GET "/api/stock/pallet/{id}/history"`
- `LazyInitializationException`
- `SQLException`

Пришлите вывод логов для дальнейшей диагностики.

## Проверка исправления

Для проверки, что код обновлен:

```bash
# Проверить timestamp компиляции
ls -la core-api/build/classes/java/main/com/wmsdipl/core/repository/PalletMovementRepository.class

# Проверить содержимое исходного файла
cat core-api/src/main/java/com/wmsdipl/core/repository/PalletMovementRepository.java | grep -A 5 "findByPalletOrderByMovedAtDesc"
```

Должно быть:
```java
@Query("SELECT pm FROM PalletMovement pm " +
       "LEFT JOIN FETCH pm.pallet " +
       "LEFT JOIN FETCH pm.fromLocation " +
       "LEFT JOIN FETCH pm.toLocation " +
       "WHERE pm.pallet = :pallet " +
       "ORDER BY pm.movedAt DESC")
List<PalletMovement> findByPalletOrderByMovedAtDesc(@Param("pallet") Pallet pallet);
```

## Альтернативный тест через curl

Протестируйте API напрямую:

```bash
# Получить список паллет
curl -u testuser:password "http://localhost:8080/api/stock?page=0&size=5"

# Получить историю паллеты с ID=2
curl -u testuser:password "http://localhost:8080/api/stock/pallet/2/history"
```

Если возвращается JSON - API работает. Если ошибка - проблема на сервере.

## Итоговый чеклист

- [ ] Код `PalletMovementRepository.java` обновлен (JOIN FETCH добавлен)
- [ ] core-api перезапущен
- [ ] В таблице `pallet_movements` есть данные (COUNT > 0)
- [ ] desktop-client перезапущен
- [ ] История паллеты открывается без ошибок

Если все пункты выполнены, но ошибка остается - предоставьте:
1. Полный текст ошибки из desktop-client
2. Логи core-api (последние 100 строк при открытии истории)
3. Результат `SELECT COUNT(*) FROM pallet_movements;`
