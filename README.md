# WMSDIPL

Много-модульный проект для модуля приходных накладных склада.

## Модули
- core-api — Spring Boot REST API, Postgres, Flyway (накладные/статусы/идемпотентность messageId).
- import-service — watch-folder через планировщик (пуллинг папки) для импорта XML → REST в core-api.
- desktop-client — JavaFX настольный клиент (без веб-UI, по договорённости).

## Текущее состояние (desktop-client)
- Экран приходов: пилюля навигации, карточка с полем поиска и кнопкой обновления, таблица с колонками `docNo`, `messageId`, `status`, `docDate`, `supplier`.
- Статус колонка подтягивается из core-api, значения отображаются без обрезки; горизонтальный скролл включён для узких окон.
- Двойной клик по документу открывает стандартное окно со строками (поля lineNo, skuId, packagingId, uom, qtyExpected, ssccExpected).
- Скроллбары минималистичные: прозрачный фон, чёрный скруглённый ползунок.
- Навигация и заголовок логотипа центрированы; кнопка выбранного модуля остаётся белой при hover.
- Шрифты: Montserrat; размеры и отступы соответствуют референс-макету.

## Текущее состояние (core-api/import-service)
- Flyway миграция V1 создаёт акты, строки, задания, сканы, расхождения, журнал импорта, историю статусов.
- Импорт XML (docs/import-template.xml) пишет акты в статус Draft, идемпотентность по messageId (уникальный индекс на receipts.message_id и import_log.message_id).
- Индексы по статусу на receipts и tasks для быстрых выборок.

## ER-диаграмма
- Исходник Mermaid: [docs/er-diagram.mmd](docs/er-diagram.mmd)
- PDF: [docs/er-diagram.pdf](docs/er-diagram.pdf)
- Основана на миграции [core-api/src/main/resources/db/migration/V1__init.sql](core-api/src/main/resources/db/migration/V1__init.sql)

## Требования
- Java 17
- Gradle 8+ (или добавьте wrapper)
- Docker (для Postgres; Keycloak секция закомментирована)

## Быстрый старт
1. Запустить Postgres: `docker compose up -d postgres`.
2. Прописать переменные окружения при необходимости: `WMS_DB_URL`, `WMS_DB_USER`, `WMS_DB_PASSWORD`, `WMS_CORE_API_PORT`, `WMS_IMPORT_PORT`, `WMS_IMPORT_FOLDER`, `WMS_CORE_API_BASE` (для import-service).
3. Собрать: `gradle build`.
4. Запуск core-api: `gradle :core-api:bootRun`.
5. Запуск import-service: `gradle :import-service:bootRun` (потребует запущенный core-api для отправки REST). Пуллит папку `wms.import.folder` раз в `wms.import.poll-interval-ms`.
6. Запуск desktop клиента: `gradle :desktop-client:run` (потребует JavaFX SDK, т.к. зависимости подтянутся из Maven).

## Импорт XML
Шаблон файла: `docs/import-template.xml`. Положите XML в папку `wms.import.folder` (по умолчанию `input`) — import-service парсит, вычисляет messageId (атрибут или SHA-256 файла) и вызывает `/api/imports` core-api. Повторный файл с тем же messageId вернёт тот же результат (идемпотентность на уровне БД).

## Безопасность
Конфигурация безопасности пока открытая (permitAll). Когда будут параметры Keycloak, добавим OIDC-конфигурацию и ограничения по ролям.

## API (черновой)
- `POST /api/imports` — импорт XML payload в Черновик (idempotent по messageId).
- `POST /api/receipts` — ручное создание Черновика.
- `POST /api/receipts/{id}/confirm` — перевод в Подтвержден.
- `POST /api/receipts/{id}/accept` — перевод в Принят (пока без проверки заданий).
- `GET /api/receipts` / `GET /api/receipts/{id}` — просмотр.

## Схема БД
Flyway миграции размещены в `core-api/src/main/resources/db/migration` и создают основные сущности (акты, строки, задания, сканы, расхождения, журнал импорта).
