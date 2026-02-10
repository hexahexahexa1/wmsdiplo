# Ролевые инструкции: приемка и cross-dock

Документ описывает актуальный operational flow по ролям (`ADMIN`, `SUPERVISOR`, `OPERATOR`, `PC_OPERATOR`) для:
- доведения приемки до конца;
- доведения cross-dock отгрузки до конца.

## ADMIN

### Полный цикл приемки
1. Создать черновик прихода: `POST /api/receipts/drafts`.
2. Добавить строки: `POST /api/receipts/{id}/lines`.
3. Перевести в `CONFIRMED`: `POST /api/receipts/{id}/confirm`.
4. Запустить приемку и генерацию receiving-задач: `POST /api/receipts/{id}/start-receiving`.
5. Контролировать выполнение receiving-задач (операторы сканируют в `POST /api/tasks/{id}/scans`).
6. При расхождениях по DRAFT/REJECTED SKU выполнить remap: `POST /api/discrepancies/{id}/remap-sku`.
7. Завершить приемку документа: `POST /api/receipts/{id}/complete-receiving`.
8. Запустить размещение: `POST /api/receipts/{id}/start-placement`.
9. Дождаться завершения placement-задач (операторы выполняют `start/scan/complete`).
10. Для обычного прихода убедиться, что документ перешел в `STOCKED`.

### Cross-dock до отгрузки
1. Выполнить шаги 1-9 выше (для `crossDock=true`).
2. После завершения placement убедиться в статусе `READY_FOR_SHIPMENT`.
3. Запустить shipping: `POST /api/receipts/{id}/start-shipping`.
4. Дождаться завершения shipping-задач.
5. Проверить финальный статус `SHIPPED` (или использовать manual fallback `POST /api/receipts/{id}/complete-shipping` при необходимости).

## SUPERVISOR

`SUPERVISOR` выполняет тот же операционный flow, что и `ADMIN` для приемки/размещения/отгрузки:
- может запускать и завершать workflow документа;
- может выполнять remap расхождений;
- может запускать placement и shipping.

Ограничение: не имеет чисто админских операций управления пользователями/частью справочников.

## OPERATOR

### Что делает в приемке
1. Получить `NEW`-задачу и назначить на себя: `POST /api/tasks/{id}/assign`.
2. Перевести задачу в работу: `POST /api/tasks/{id}/start`.
3. Выполнять сканы: `POST /api/tasks/{id}/scans`.
4. При ошибочном последнем скане выполнить откат: `POST /api/tasks/{id}/undo-last-scan`.
5. При необходимости сбросить задачу (rollback всех сканов задачи): `POST /api/tasks/{id}/release`.
6. Завершить задачу: `POST /api/tasks/{id}/complete`.

### Что делает в placement/shipping
- Выполняет те же действия `assign -> start -> scans -> undo/release (при необходимости) -> complete`.
- Не запускает document-level переходы (`start-placement`, `start-shipping`, `complete-receiving`) — это зона `ADMIN`/`SUPERVISOR`.

## PC_OPERATOR

`PC_OPERATOR` не завершает workflow самостоятельно, но критичен как контрольная роль:
1. Проверяет справочники зон/ячеек и доступность размещения (`/api/zones`, `/api/locations`).
2. Контролирует паллеты и остатки (`/api/pallets`, `/api/stock`).
3. Мониторит документы и статусы (`/api/receipts`).
4. Эскалирует блокировки по capacity/статусам `ADMIN` или `SUPERVISOR` для запуска/завершения этапов.

## Критичные правила

1. Remap DRAFT SKU должен быть сделан до `complete-receiving`, иначе документ-level блокер вернет `409`.
2. `Task complete` для последней receiving-задачи не должен падать из-за SKU blockers; блокировка относится к документному переходу.
3. `undo-last-scan` разрешен только для задач в `ASSIGNED`/`IN_PROGRESS`; для `COMPLETED` возвращается `409`.
4. Перед `start-placement` должна быть емкость в целевых `STORAGE`/`CROSS_DOCK` ячейках.
