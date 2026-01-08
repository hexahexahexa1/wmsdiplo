# Исправление: Обнаружение расхождений UNDER_QTY (недостачи)

## Проблема

При приемке товара **с недостачей** (когда принято меньше чем ожидалось):
- ❌ Система НЕ создавала запись о расхождении
- ❌ В колонке "Расхождение" показывалось "✓ ОК" вместо "⚠ Есть расхождение"
- ❌ Диалог подтверждения расхождений НЕ появлялся
- ❌ Приход завершался как будто все в порядке

## Причина

В `ReceivingWorkflowService.recordScan()` были проверки только для:
- ✅ `BARCODE_MISMATCH` - неправильный баркод
- ✅ `OVER_QTY` - приняли больше чем ожидалось
- ✅ `SSCC_MISMATCH` - неправильный SSCC

**НО отсутствовала проверка**:
- ❌ `UNDER_QTY` - приняли меньше чем ожидалось

## Решение

### Изменения в `TaskLifecycleService.complete()`

Добавлена логика обнаружения недостачи при **завершении задачи**:

```java
@Transactional
public Task complete(Long id) {
    Task task = getTask(id);
    
    // Проверка на UNDER_QTY расхождение
    BigDecimal qtyDone = task.getQtyDone() != null ? task.getQtyDone() : BigDecimal.ZERO;
    BigDecimal qtyAssigned = task.getQtyAssigned();
    
    if (qtyAssigned != null && qtyDone.compareTo(qtyAssigned) < 0) {
        // Создаем запись о расхождении UNDER_QTY
        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setReceipt(receipt);
        discrepancy.setLine(line);
        discrepancy.setType("UNDER_QTY");
        discrepancy.setQtyExpected(qtyAssigned);
        discrepancy.setQtyActual(qtyDone);
        discrepancy.setComment("Task completed with less quantity than assigned");
        discrepancyRepository.save(discrepancy);
        
        // Помечаем последний скан флагом discrepancy для отображения в UI
        List<Scan> scans = scanRepository.findByTask(task);
        if (!scans.isEmpty()) {
            Scan lastScan = scans.get(scans.size() - 1);
            lastScan.setDiscrepancy(true);
            scanRepository.save(lastScan);
        }
    }
    
    task.setStatus(TaskStatus.COMPLETED);
    task.setClosedAt(LocalDateTime.now());
    return taskRepository.save(task);
}
```

### Почему проверка при завершении, а не при скане?

**UNDER_QTY** определяется только при завершении задачи, потому что:
1. Во время отдельного скана мы не знаем, будет ли оператор продолжать сканировать
2. Оператор может сделать несколько сканов и постепенно набрать нужное количество
3. Только при нажатии кнопки "Завершить" мы знаем окончательное количество

**Другие расхождения** (OVER_QTY, BARCODE_MISMATCH, SSCC_MISMATCH) определяются **сразу при скане**, потому что:
- Превышение сразу видно: `newTotal > expectedQty`
- Неправильный баркод/SSCC сразу виден при сравнении

## Измененные файлы

### 1. `core-api/src/main/java/com/wmsdipl/core/service/TaskLifecycleService.java`

**Добавлены зависимости**:
```java
import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.Scan;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.ScanRepository;
import java.math.BigDecimal;
import java.util.List;
```

**Расширен конструктор**:
```java
public TaskLifecycleService(
        TaskRepository taskRepository,
        ScanRepository scanRepository,
        DiscrepancyRepository discrepancyRepository
) {
    this.taskRepository = taskRepository;
    this.scanRepository = scanRepository;
    this.discrepancyRepository = discrepancyRepository;
}
```

**Метод `complete()` расширен** логикой обнаружения UNDER_QTY (см. выше).

## Как работает обнаружение расхождений теперь

### Во время сканирования (`ReceivingWorkflowService.recordScan()`)

Сразу проверяется:
- ✅ **BARCODE_MISMATCH**: `barcode != expectedSku.code`
- ✅ **OVER_QTY**: `newTotal > expectedQty`
- ✅ **SSCC_MISMATCH**: `sscc != expectedSscc`

Если найдено → создается `Discrepancy` запись, `scan.discrepancy = true`

### При завершении задачи (`TaskLifecycleService.complete()`)

**НОВОЕ**: проверяется:
- ✅ **UNDER_QTY**: `qtyDone < qtyAssigned`

Если найдено:
1. Создается `Discrepancy` запись
2. **Последний скан** помечается `discrepancy = true` (чтобы в UI показывалось предупреждение)
3. Метод `TaskService.hasDiscrepancies()` вернет `true`
4. Диалог подтверждения появится перед завершением

### Проверка перед завершением (`TaskService.hasDiscrepancies()`)

```java
public boolean hasDiscrepancies(Long taskId) {
    Task task = taskLifecycleService.getTask(taskId);
    List<Scan> scans = scanRepository.findByTask(task);
    return scans.stream().anyMatch(scan -> 
        scan.getDiscrepancy() != null && scan.getDiscrepancy()
    );
}
```

Этот метод вернет `true` если **хотя бы один скан** имеет `discrepancy = true`.

## Тестирование

### Подготовка

1. Запустите окружение:
```bash
start-environment.bat     # PostgreSQL
start-core-api.bat        # Backend
start-desktop-client.bat  # Desktop UI
```

2. Войдите в систему:
   - Логин: `admin`
   - Пароль: `password`

### Сценарий 1: Приемка с недостачей (UNDER_QTY)

1. **Найдите приход** RCP-NEW-SKU-001 (статус DRAFT)
2. **Подтвердите** → Начать приемку
3. **Перейдите** на вкладку "Терминал"
4. **Откройте задачу** → Назначить → Начать
5. **Отсканируйте товар** с МЕНЬШИМ количеством:
   - Паллета: `PLT-NEW-001`
   - Баркод: `NEW-SKU-2026`
   - Количество: **50** (вместо ожидаемых 100)
   - Нажмите "Записать скан"
6. **Нажмите кнопку "Завершить"**

**Ожидаемый результат**:
- ✅ В таблице сканов последний скан должен показывать **"⚠ Есть расхождение"**
- ✅ Появится диалог подтверждения:
  ```
  Обнаружены расхождения при приёмке!
  
  Вы уверены, что хотите завершить задание с расхождениями?
  После завершения приход может потребовать разрешения расхождений.
  
  [Подтвердить завершение] [Отменить и вернуться к приёмке]
  ```
- ✅ При подтверждении: задача завершится, статус прихода → PENDING_RESOLUTION
- ✅ При отмене: вернет на форму сканирования, можно досканировать

### Сценарий 2: Приемка с превышением (OVER_QTY)

1. **Откройте задачу**
2. **Отсканируйте** с БОЛЬШИМ количеством:
   - Количество: **150** (вместо 100)
3. **Проверьте таблицу сканов**

**Ожидаемый результат**:
- ✅ Сразу после скана показывается **"⚠ Есть расхождение"**
- ✅ При завершении появится диалог подтверждения

### Сценарий 3: Приемка точно по документу (без расхождений)

1. **Откройте задачу**
2. **Отсканируйте** ТОЧНОЕ количество:
   - Количество: **100** (ровно как ожидалось)
3. **Нажмите "Завершить"**

**Ожидаемый результат**:
- ✅ В таблице сканов показывается **"✓ ОК"**
- ✅ Диалог НЕ появляется
- ✅ Задача сразу завершается
- ✅ Если все задачи завершены без расхождений → статус прихода ACCEPTED

## Проверка в базе данных

После теста с недостачей:

```sql
-- Проверить расхождения
SELECT * FROM discrepancies WHERE receipt_id = 28;

-- Должно показать:
-- type = 'UNDER_QTY'
-- qty_expected = 100
-- qty_actual = 50
-- comment = 'Task completed with less quantity than assigned'

-- Проверить сканы
SELECT id, pallet_code, qty, discrepancy FROM scans WHERE task_id = <task_id>;

-- Последний скан должен иметь discrepancy = true
```

## Статусы прихода после завершения

### Без расхождений:
```
IN_PROGRESS → ACCEPTED
```

### С расхождениями (UNDER_QTY, OVER_QTY, BARCODE_MISMATCH, SSCC_MISMATCH):
```
IN_PROGRESS → PENDING_RESOLUTION
```

### Разрешение расхождений:
```sql
-- Пометить расхождение как разрешенное
UPDATE discrepancies SET resolved = true, comment = 'Approved by supervisor' WHERE id = <id>;

-- Повторить попытку завершения прихода
POST /api/receipts/{id}/complete-receiving
```

Если все расхождения разрешены:
```
PENDING_RESOLUTION → ACCEPTED
```

## Важные замечания

1. **UNDER_QTY определяется только при завершении задачи**, не во время отдельного скана
2. **Последний скан помечается** флагом `discrepancy = true` для отображения в UI
3. **Диалог подтверждения** показывается для ВСЕХ типов расхождений
4. **Приход переходит в PENDING_RESOLUTION** если есть хотя бы одно неразрешенное расхождение

## Связанные файлы

- `core-api/src/main/java/com/wmsdipl/core/service/TaskLifecycleService.java` - обнаружение UNDER_QTY
- `core-api/src/main/java/com/wmsdipl/core/service/workflow/ReceivingWorkflowService.java` - обнаружение других расхождений
- `core-api/src/main/java/com/wmsdipl/core/service/TaskService.java` - метод hasDiscrepancies()
- `desktop-client/src/main/java/com/wmsdipl/desktop/DesktopClientApplication.java` - диалог подтверждения
- `DISCREPANCY_CONFIRMATION_TEST.md` - подробная инструкция по тестированию

## Дата изменения

08 января 2026
