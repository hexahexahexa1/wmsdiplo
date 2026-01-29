# Руководство по развертыванию WMSDIPL на новой машине

Это руководство поможет вам развернуть проект WMSDIPL (Warehouse Management System) на абсолютно новой машине, сохранив данные из текущей базы данных.

## Обзор проекта

WMSDIPL - много-модульное Java-приложение для управления складскими приходами:

- **core-api**: Spring Boot REST API (порт 8080)
- **import-service**: Сервис импорта XML файлов (порт 8090)
- **desktop-client**: JavaFX настольный клиент
- **shared-contracts**: Общие DTO и контракты

## Предварительные требования

### Необходимое ПО

1. **Java 17 JDK** (обязательно)
   - Скачайте с [oracle.com](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) или [adoptium.net](https://adoptium.net/)
   - Установите и проверьте: `java -version`

2. **Gradle** (рекомендуется 8+)
   - Скачайте с [gradle.org](https://gradle.org/install/)
   - Или используйте Gradle Wrapper (включен в проект)
   - Проверьте: `gradle -v`

3. **Docker Desktop**
   - Скачайте с [docker.com](https://www.docker.com/products/docker-desktop/)
   - Установите и запустите Docker Desktop
   - Проверьте: `docker --version`

4. **Git**
   - Скачайте с [git-scm.com](https://git-scm.com/downloads)
   - Установите и проверьте: `git --version`

### Системные требования

- **ОС**: Windows 10/11, Linux, или macOS
- **RAM**: Минимум 4GB, рекомендуется 8GB+
- **Диск**: Минимум 5GB свободного места
- **Порты**: Убедитесь, что порты 8080, 8090 и 55432 свободны

## Шаг 1: Сохранение данных текущей базы

### На исходной машине (с запущенной системой)

1. **Убедитесь, что PostgreSQL запущен:**
   ```bash
   docker ps | findstr wmsdipl-postgres
   ```

2. **Создайте полный дамп базы данных:**
   ```bash
   # Windows
   docker exec wmsdipl-postgres pg_dump -U wmsdipl -d wmsdipl --no-owner --no-acl > wmsdipl_backup.sql

   # Linux/Mac
   docker exec wmsdipl-postgres pg_dump -U wmsdipl -d wmsdipl --no-owner --no-acl > wmsdipl_backup.sql
   ```

3. **Проверьте, что файл создан:**
   ```bash
   dir wmsdipl_backup.sql  # Windows
   ls -la wmsdipl_backup.sql  # Linux/Mac
   ```

4. **Скопируйте файл `wmsdipl_backup.sql` на новую машину**

> **Примечание**: Этот файл содержит все данные - приходы, товары, паллеты, задания и т.д.

## Шаг 2: Подготовка новой машины

### Установка JDK 17

```bash
# Скачайте и установите JDK 17
# Проверьте установку
java -version
# Должно вывести что-то вроде: java version "17.0.8"
```

### Установка Gradle (опционально, если не используете wrapper)

```bash
# Скачайте и установите Gradle
# Проверьте установку
gradle -v
```

### Установка Docker

```bash
# Скачайте и установите Docker Desktop
# Запустите Docker Desktop
# Проверьте установку
docker --version
docker compose version
```

### Установка Git

```bash
# Скачайте и установите Git
# Проверьте установку
git --version
```

## Шаг 3: Клонирование и настройка проекта

### Клонируйте репозиторий

```bash
# Клонируйте проект
git clone https://github.com/your-repo/wmsdipl.git  # Замените на актуальный URL
cd wmsdipl
```

### Скопируйте файл с данными

```bash
# Скопируйте wmsdipl_backup.sql в корень проекта
# Файл должен быть в папке wmsdipl/
```

## Шаг 4: Настройка базы данных

### Запустите PostgreSQL

```bash
# Запустите PostgreSQL через Docker Compose
docker compose up -d postgres
```

### Дождитесь готовности базы

```bash
# Проверьте, что контейнер запущен и здоров
docker ps

# Подождите 30-60 секунд, пока PostgreSQL инициализируется
# Проверьте логи если нужно:
docker logs wmsdipl-postgres
```

### Восстановите данные

```bash
# Пересоздайте базу данных (на случай если она существует)
docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "DROP DATABASE IF EXISTS wmsdipl;"
docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "CREATE DATABASE wmsdipl;"

# Восстановите схему и данные из бэкапа
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl < wmsdipl_backup.sql
```

> **Важно**: Убедитесь, что файл `wmsdipl_backup.sql` находится в текущей директории

## Шаг 5: Сборка проекта

### Используя Gradle (рекомендуется)

```bash
# Сборка всех модулей
gradle build

# Или используя wrapper (работает без установки Gradle)
./gradlew build  # Linux/Mac
gradlew.bat build  # Windows
```

### Проверьте сборку

```bash
# Должно завершиться без ошибок
# Проверьте созданные JAR файлы
dir build/libs/  # Windows
ls -la */build/libs/  # Linux/Mac
```

## Шаг 6: Запуск приложений

### Вариант 1: Запуск всех сервисов автоматически

```bash
# Windows
start-all.bat

# Linux/Mac - запустите по отдельности или создайте скрипт
```

### Вариант 2: Ручной запуск по отдельности

1. **Запустите Core API (порт 8080):**
   ```bash
   gradle :core-api:bootRun
   # Или
   ./gradlew :core-api:bootRun
   ```

2. **Запустите Import Service (порт 8090):**
   ```bash
   gradle :import-service:bootRun
   # Или
   ./gradlew :import-service:bootRun
   ```

3. **Запустите Desktop Client:**
   ```bash
   gradle :desktop-client:run
   # Или
   ./gradlew :desktop-client:run
   ```

### Вариант 3: Запуск JAR файлов (для production)

```bash
# Собрать JAR файлы
gradle bootJar

# Запустить Core API
java -jar core-api/build/libs/core-api-0.1.0-SNAPSHOT.jar

# Запустить Import Service
java -jar import-service/build/libs/import-service-0.1.0-SNAPSHOT.jar

# Desktop Client запускается через Gradle
gradle :desktop-client:run
```

## Шаг 7: Проверка работы

### Проверьте доступность API

```bash
# Проверьте Core API
curl http://localhost:8080/actuator/health

# Проверьте Import Service
curl http://localhost:8090/actuator/health

# Проверьте API endpoints
curl http://localhost:8080/api/receipts -u admin:admin
```

### Проверьте Desktop Client

- Должен открыться JavaFX интерфейс
- Войдите с логином `admin` / `admin`
- Проверьте, что данные восстановлены (приходы, товары и т.д.)

### Проверьте импорт XML

```bash
# Положите тестовый XML файл в папку input/
# Проверьте логи Import Service
```

## Переменные окружения (опционально)

Если нужно настроить порты или подключения, создайте файл `.env`:

```bash
# База данных
WMS_DB_URL=jdbc:postgresql://localhost:55432/wmsdipl
WMS_DB_USER=wmsdipl
WMS_DB_PASSWORD=wmsdipl

# Порты
WMS_CORE_API_PORT=8080
WMS_IMPORT_PORT=8090

# Import Service настройки
WMS_IMPORT_FOLDER=input
WMS_IMPORT_POLL_MS=2000
WMS_CORE_API_BASE=http://localhost:8080
```

## Устранение проблем

### PostgreSQL не запускается

```bash
# Проверьте логи
docker logs wmsdipl-postgres

# Перезапустите
docker compose down
docker compose up -d postgres
```

### Сборка падает

```bash
# Очистите кэш Gradle
gradle clean

# Проверьте Java версию
java -version

# Проверьте Gradle версию
gradle -v
```

### Приложения не запускаются

```bash
# Проверьте, что порты свободны
netstat -ano | findstr :8080  # Windows
lsof -i :8080  # Linux/Mac

# Проверьте логи приложений
# Core API логи в консоли
# Import Service логи в консоли
```

### Данные не восстановились

```bash
# Проверьте файл бэкапа
head -20 wmsdipl_backup.sql

# Проверьте подключение к БД
docker exec -it wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "SELECT COUNT(*) FROM receipts;"

# Перевосстановите если нужно
docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "DROP DATABASE IF EXISTS wmsdipl;"
docker exec wmsdipl-postgres psql -U wmsdipl -d postgres -c "CREATE DATABASE wmsdipl;"
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl < wmsdipl_backup.sql
```

## Структура проекта после развертывания

```
wmsdipl/
├── shared-contracts/          # Общие DTO
├── core-api/                  # REST API (порт 8080)
├── import-service/            # Импорт XML (порт 8090)
├── desktop-client/            # JavaFX клиент
├── database/                  # Скрипты БД
│   ├── init_schema.sql       # Схема БД
│   └── README.md             # Документация БД
├── docs/                     # Документация
├── input/                    # Папка для XML импорта
├── wmsdipl_backup.sql        # Ваш бэкап данных
└── docker-compose.yml        # Конфигурация PostgreSQL
```

## Полезные команды

```bash
# Остановка всех сервисов
docker compose down

# Просмотр логов
docker logs wmsdipl-postgres
gradle :core-api:bootRun --console=plain

# Перезапуск приложений
gradle :core-api:bootRun
gradle :import-service:bootRun
gradle :desktop-client:run

# Создание нового бэкапа
docker exec wmsdipl-postgres pg_dump -U wmsdipl -d wmsdipl --no-owner --no-acl > wmsdipl_backup_new.sql
```

## Поддержка

- **Документация**: См. `README.md`, `AGENTS.md`
- **API документация**: http://localhost:8080/swagger-ui.html
- **Логи**: Проверяйте консоль приложений и `docker logs wmsdipl-postgres`

---

**Примечание**: Это руководство предполагает стандартную конфигурацию. При необходимости скорректируйте порты и настройки под вашу среду.</content>
<parameter name="filePath">E:\WMSDIPL\DEPLOYMENT_GUIDE.md