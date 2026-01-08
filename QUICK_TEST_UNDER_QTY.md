# Краткая инструкция: Тестирование исправления UNDER_QTY

## Что исправлено

✅ **Теперь система обнаруживает недостачу (UNDER_QTY)** при завершении задачи с меньшим количеством чем ожидалось

✅ **Показывает "⚠ Есть расхождение"** в таблице сканов

✅ **Выводит диалог подтверждения** перед завершением задачи с расхождениями

## Быстрый тест

### 1. Перезапустите core-api

Если core-api уже запущен, остановите и запустите снова:

```bash
# Найти процесс
netstat -ano | findstr :8080

# Остановить (замените PID на реальный)
taskkill //F //PID <PID>

# Запустить
start-core-api.bat
# ИЛИ
gradle :core-api:bootRun
```

Дождитесь запуска (15-30 секунд).

### 2. Откройте desktop-client

Если уже запущен - оставьте как есть, если нет:

```bash
start-desktop-client.bat
# ИЛИ
gradle :desktop-client:run
```

### 3. Тест недостачи

1. **Войдите**: admin / password
2. **Найдите приход**: RCP-NEW-SKU-001 (должен быть в статусе DRAFT)
3. **Подтвердите** → "Начать приемку"
4. **Терминал** → откройте задачу
5. **Назначить** → **Начать**
6. **Сканируйте**:
   - Паллета: `PLT-NEW-001`
   - Баркод: `NEW-SKU-2026`
   - **Количество: 50** (меньше чем 100 ожидается!)
   - Нажмите "Записать скан"

7. **Проверьте таблицу**: пока должно быть "✓ ОК"

8. **Нажмите "Завершить"**

### ✅ Ожидаемый результат

После нажатия "Завершить":

1. **Таблица обновится** → последний скан покажет **"⚠ Есть расхождение"**
2. **Появится диалог**:
   ```
   Обнаружены расхождения при приёмке!
   
   [Подтвердить завершение] [Отменить]
   ```
3. **При подтверждении**:
   - Задача → COMPLETED
   - Приход → PENDING_RESOLUTION (если все задачи завершены)

### Проверка в БД

```sql
-- Подключение
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl

-- Проверить расхождения
SELECT id, type, qty_expected, qty_actual, comment 
FROM discrepancies 
WHERE receipt_id = 28;

-- Должно показать UNDER_QTY с qty_expected=100, qty_actual=50

-- Проверить последний скан
SELECT id, pallet_code, qty, discrepancy 
FROM scans 
WHERE task_id IN (SELECT id FROM tasks WHERE receipt_id = 28)
ORDER BY scanned_at DESC 
LIMIT 1;

-- discrepancy должно быть true
```

## Сброс данных для повторного теста

Если нужно протестировать еще раз:

```sql
-- Откатить приход обратно в DRAFT
UPDATE receipts SET status = 'DRAFT' WHERE doc_no = 'RCP-NEW-SKU-001';

-- Удалить задачи
DELETE FROM tasks WHERE receipt_id = 28;

-- Удалить сканы
DELETE FROM scans WHERE task_id IN (SELECT id FROM tasks WHERE receipt_id = 28);

-- Удалить расхождения
DELETE FROM discrepancies WHERE receipt_id = 28;

-- Удалить паллеты
DELETE FROM pallets WHERE receipt_id = 28;
```

## Проблемы?

### Диалог НЕ появляется

1. Проверьте логи core-api - есть ли ошибки?
2. Проверьте БД - создалась ли запись в discrepancies?
3. Проверьте БД - последний скан имеет discrepancy = true?

### В таблице показывает "ОК" вместо "⚠ Есть расхождение"

1. Обновите таблицу (переоткройте задачу)
2. Проверьте в БД: `SELECT discrepancy FROM scans WHERE id = <scan_id>;`

### Core-API не запускается

```bash
# Проверить логи
gradle :core-api:bootRun

# Проверить компиляцию
gradle :core-api:compileJava
```

## Измененные файлы

- `core-api/src/main/java/com/wmsdipl/core/service/TaskLifecycleService.java`

## Полная документация

См. `UNDER_QTY_DISCREPANCY_FIX.md` для подробного объяснения изменений.
