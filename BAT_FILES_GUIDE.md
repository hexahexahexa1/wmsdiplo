# BAT Файлы для Управления WMSDIPL

## Быстрый Запуск

### Запустить все сервисы автоматически
```cmd
start-all.bat
```

**Что делает**:
1. Проверяет PostgreSQL (запускает если не запущен)
2. Запускает Core API на порту 8080
3. Запускает Import Service на порту 8090
4. Запускает Desktop Client
5. Все сервисы открываются в отдельных окнах

**Время запуска**: ~20-30 секунд

---

### Остановить все сервисы
```cmd
stop-all.bat
```

**Что делает**:
1. Останавливает Core API (порт 8080)
2. Останавливает Import Service (порт 8090)
3. Останавливает Desktop Client

**Примечание**: PostgreSQL продолжает работать. Чтобы остановить PostgreSQL:
```cmd
docker compose down
```

---

## Запуск Отдельных Сервисов

### PostgreSQL
```cmd
start-postgres.bat
```
- Запускает PostgreSQL в Docker контейнере
- Порт: 55432 (внешний), 5432 (внутренний)
- База данных: wmsdipl
- Пользователь: wmsdipl / wmsdipl

### Core API
```cmd
start-core-api.bat
```
- Запускает основной REST API
- Порт: 8080
- URL: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

### Import Service
```cmd
start-import-service.bat
```
- Запускает сервис импорта XML файлов
- Порт: 8090
- Папка импорта: `input/`
- Интервал проверки: каждые 2 секунды
- Автоматически отправляет данные в Core API

### Desktop Client
```cmd
start-desktop-client.bat
```
- Запускает JavaFX настольное приложение
- Требует: Core API должен быть запущен
- Логин: testuser / password

---

## Последовательность Запуска

### Полный запуск с нуля
```cmd
# 1. Запустить PostgreSQL
start-postgres.bat

# 2. Подождать 5 секунд
timeout /t 5

# 3. Запустить Core API
start-core-api.bat

# 4. Подождать 10 секунд (пока Core API запустится)
timeout /t 10

# 5. (Опционально) Запустить Import Service
start-import-service.bat

# 6. Запустить Desktop Client
start-desktop-client.bat
```

**ИЛИ просто используйте**:
```cmd
start-all.bat
```
Он делает все эти шаги автоматически!

---

## Проверка Статуса

### Проверить Core API
```cmd
curl http://localhost:8080/actuator/health
```
Ожидаемый результат: `{"status":"UP"}`

### Проверить Import Service
```cmd
curl http://localhost:8090/actuator/health
```
(если в Import Service есть actuator endpoint)

### Проверить PostgreSQL
```cmd
docker ps | findstr wmsdipl-postgres
```
Должна быть строка с контейнером wmsdipl-postgres

### Проверить занятые порты
```cmd
netstat -ano | findstr :8080
netstat -ano | findstr :8090
```

---

## Остановка Сервисов

### Автоматическая остановка всех сервисов
```cmd
stop-all.bat
```

### Ручная остановка

#### Остановить Core API
```cmd
# Найти процесс
netstat -ano | findstr :8080

# Остановить (замените PID на номер процесса)
taskkill /F /PID <PID>
```

#### Остановить Import Service
```cmd
# Найти процесс
netstat -ano | findstr :8090

# Остановить (замените PID на номер процесса)
taskkill /F /PID <PID>
```

#### Остановить PostgreSQL
```cmd
docker compose down
```

---

## Типичные Проблемы и Решения

### Проблема: "Порт 8080 уже занят"
**Решение**: Остановите процесс на порту 8080
```cmd
for /f "tokens=5" %a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do taskkill /F /PID %a
```

### Проблема: "PostgreSQL не запускается"
**Решение**: Проверьте Docker
```cmd
docker ps -a
docker compose up -d postgres
```

### Проблема: "Desktop Client не подключается к API"
**Решение**: 
1. Проверьте, что Core API запущен: `curl http://localhost:8080/actuator/health`
2. Если не запущен, запустите: `start-core-api.bat`

### Проблема: "Import Service не видит файлы в input/"
**Решение**:
1. Убедитесь, что папка `input/` существует в корне проекта
2. Проверьте права доступа к папке
3. Проверьте логи Import Service

---

## Логи и Отладка

### Просмотр логов Core API
- Логи отображаются в окне консоли
- Для сохранения в файл добавьте перенаправление в `start-core-api.bat`:
```cmd
gradle :core-api:bootRun > logs/core-api.log 2>&1
```

### Просмотр логов Import Service
- Логи отображаются в окне консоли
- Для сохранения в файл добавьте перенаправление в `start-import-service.bat`:
```cmd
gradle :import-service:bootRun > logs/import-service.log 2>&1
```

### Просмотр логов Desktop Client
- Логи сохраняются в файл `DEBUG.txt` в корне проекта
- Открыть: `notepad DEBUG.txt`

---

## Переменные Окружения

### Core API
- `WMS_DB_HOST` - хост PostgreSQL (по умолчанию: localhost)
- `WMS_DB_PORT` - порт PostgreSQL (по умолчанию: 55432)

### Import Service
- `WMS_IMPORT_FOLDER` - папка для импорта (по умолчанию: input)
- `WMS_IMPORT_POLL_MS` - интервал проверки в мс (по умолчанию: 2000)
- `WMS_CORE_API_BASE` - URL Core API (по умолчанию: http://localhost:8080)
- `WMS_IMPORT_PORT` - порт Import Service (по умолчанию: 8090)

### Desktop Client
- `WMS_CORE_API_BASE` - URL Core API (по умолчанию: http://localhost:8080)

**Пример установки переменных перед запуском**:
```cmd
set WMS_IMPORT_FOLDER=C:\import-files
set WMS_IMPORT_POLL_MS=5000
start-import-service.bat
```

---

## Рекомендуемые Сценарии

### Разработка (Development)
```cmd
# Запустить только PostgreSQL и Core API
start-postgres.bat
start-core-api.bat

# Использовать Gradle напрямую для Desktop Client (быстрый перезапуск)
gradle :desktop-client:run
```

### Тестирование импорта (Import Testing)
```cmd
# Запустить все сервисы
start-all.bat

# Положить тестовый XML файл в input/
copy docs\import-template.xml input\test-001.xml

# Проверить логи Import Service
# Проверить, что накладная появилась в Desktop Client
```

### Полное окружение (Full Environment)
```cmd
# Запустить все сервисы автоматически
start-all.bat

# Проверить статус всех сервисов
curl http://localhost:8080/actuator/health
curl http://localhost:8090/actuator/health
docker ps | findstr wmsdipl-postgres
```

### Перезапуск после изменений кода
```cmd
# Остановить все
stop-all.bat

# Пересобрать проект
gradle clean build

# Запустить заново
start-all.bat
```

---

## Дополнительные Команды

### Проверить версию Java
```cmd
java -version
```
Требуется: Java 17

### Проверить версию Gradle
```cmd
gradle -version
```
Требуется: Gradle 8+

### Собрать проект без запуска
```cmd
gradle build
```

### Собрать только один модуль
```cmd
gradle :core-api:build
gradle :import-service:build
gradle :desktop-client:build
```

### Запустить тесты
```cmd
gradle test
gradle :core-api:test
```

---

## Структура BAT Файлов

```
E:\WMSDIPL\
├── start-all.bat          ← Запуск всех сервисов
├── stop-all.bat           ← Остановка всех сервисов
├── start-postgres.bat     ← PostgreSQL
├── start-core-api.bat     ← Core API
├── start-import-service.bat  ← Import Service (НОВЫЙ)
└── start-desktop-client.bat  ← Desktop Client
```

---

## Быстрая Справка

| Задача | Команда |
|--------|---------|
| Запустить все | `start-all.bat` |
| Остановить все | `stop-all.bat` |
| Только Core API | `start-core-api.bat` |
| Только Import Service | `start-import-service.bat` |
| Только Desktop Client | `start-desktop-client.bat` |
| Проверить Core API | `curl http://localhost:8080/actuator/health` |
| Проверить Import Service | `curl http://localhost:8090/actuator/health` |
| Проверить PostgreSQL | `docker ps \| findstr wmsdipl-postgres` |
| Остановить порт 8080 | `for /f "tokens=5" %a in ('netstat -ano ^| findstr :8080') do taskkill /F /PID %a` |
| Остановить PostgreSQL | `docker compose down` |

---

## Поддержка

Если возникли проблемы:
1. Проверьте логи в окнах консоли
2. Проверьте `DEBUG.txt` для Desktop Client
3. Проверьте статус сервисов (см. раздел "Проверка Статуса")
4. Используйте `stop-all.bat` и `start-all.bat` для полного перезапуска

---

**Создано**: Январь 2026  
**Версия**: 1.0
