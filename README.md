# WMSDIPL

Мультимодульная WMS-система на Java 17 / Spring Boot 3 / Gradle.

## Модули
- `core-api` - REST API и бизнес-логика склада.
- `import-service` - импорт XML в `core-api`.
- `desktop-client` - JavaFX клиент (Tasks, Terminal, Analytics).
- `shared-contracts` - общие DTO (records).

## Требования
- Java 17
- Docker
- Gradle Wrapper (`gradlew.bat`)

## Быстрый старт
1. Поднять PostgreSQL: `docker compose up -d postgres`
2. Собрать проект: `gradlew.bat clean build`
3. Запустить API: `gradlew.bat :core-api:bootRun`
4. Запустить Desktop: `gradlew.bat :desktop-client:run`

## Что актуально в текущем релизе
- Поиск задач по `taskId` с префиксной фильтрацией (по мере ввода) в API + Desktop (`Tasks` и `Terminal`).
- `Complete Task` для RECEIVING не ломается при document-level SKU blocker (DRAFT/REJECTED SKU): задача завершается, документ не продвигается до remap/resolve.
- `Undo last scan` для `RECEIVING` / `PLACEMENT` / `SHIPPING` с откатом связанных фактов.
- Безопасный `release` задачи через полный rollback сканов.
- Remap discrepancy SKU учитывается при генерации placement-задач (используются remap-номенклатуры).
- Analytics pane в Desktop с гарантированно видимым вертикальным скроллом.

## Автотест API flow
Сохраненный regression-скрипт:
- `scripts/testing/api_full_regression_v2.ps1`

Запуск:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\testing\api_full_regression_v2.ps1
```

Скрипт проверяет:
- analytics (валидные/невалидные диапазоны);
- фильтр `taskId` по префиксу;
- receiving + remap + placement flow;
- `undo-last-scan` и `release` rollback;
- cross-dock receiving -> placement -> shipping;
- базовые security-проверки (`401/403`).

## Роли и операционные инструкции
Актуальный role-by-role runbook:
- `docs/ROLE_WORKFLOWS.md`

В документе есть пошаговые сценарии для ролей:
- `ADMIN`
- `SUPERVISOR`
- `OPERATOR`
- `PC_OPERATOR`

Отдельно описаны пути:
- как довести приемку до конца;
- как довести cross-dock до финальной отгрузки.

## Полезные команды
```powershell
# Тесты
./gradlew.bat :core-api:test
./gradlew.bat :desktop-client:test

# Полная проверка
./gradlew.bat check

# Swagger
# http://localhost:8080/swagger-ui.html
```
