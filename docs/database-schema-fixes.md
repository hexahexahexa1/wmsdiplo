# Исправление схемы базы данных для таблицы users

## Дата: 2026-01-12

## Проблема
При создании пользователя через desktop-client возникала ошибка:
```
ERROR: column "role" is of type user_role but expression is of type character varying
```

## Причина
Таблица `users` в PostgreSQL использовала ENUM тип `user_role` для колонки `role`, 
в то время как JPA Entity использует `@Enumerated(EnumType.STRING)`, который записывает 
значения как строки VARCHAR.

### Было:
```sql
role  user_role  NOT NULL  DEFAULT 'OPERATOR'::user_role
```

### Стало:
```sql
role  VARCHAR(32)  NOT NULL  DEFAULT 'OPERATOR'
```

## Примененные изменения в схеме БД

```sql
-- 1. Конвертировали колонку role из ENUM в VARCHAR
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(32) USING role::text;

-- 2. Удалили default (для удаления зависимости от ENUM)
ALTER TABLE users ALTER COLUMN role DROP DEFAULT;

-- 3. Удалили ENUM тип
DROP TYPE IF EXISTS user_role;

-- 4. Восстановили default со строковым значением
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'OPERATOR';
```

## Примененные изменения в коде

### 1. UserController.java
- ✅ Добавлена аннотация `@Valid` для валидации запросов
- ✅ Добавлен импорт `jakarta.validation.Valid`

### 2. UserRequest.java
- ✅ Добавлены валидационные аннотации:
  - `@NotBlank(message = "Username is required")`
  - `@Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")`

### 3. UserMapper.java
- ✅ Гарантированная установка role по умолчанию (OPERATOR)
- ✅ Гарантированная установка active по умолчанию (true)

### 4. UserService.java
- ✅ Проверка уникальности username ДО попытки сохранения в БД
- ✅ Обработка DataIntegrityViolationException
- ✅ Подробное логирование (INFO, DEBUG, ERROR)
- ✅ Понятные сообщения об ошибках

### 5. GlobalExceptionHandler.java (новый файл)
- ✅ Централизованная обработка всех исключений
- ✅ Стандартизированный формат ответов об ошибках
- ✅ Специальная обработка валидационных ошибок

## Проверка работоспособности

### Через API:
```bash
curl -X POST http://localhost:8080/api/users \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "fullName": "New User",
    "role": "OPERATOR"
  }'
```

Ожидаемый результат:
```json
{
  "id": 3,
  "username": "newuser",
  "fullName": "New User",
  "email": null,
  "role": "OPERATOR",
  "active": true
}
```

### Через Desktop Client:
1. Запустить: `gradle :desktop-client:run`
2. Войти: `admin` / `admin`
3. Перейти в раздел "Пользователи"
4. Нажать "+ Создать пользователя"
5. Заполнить форму и создать пользователя
6. Ожидаемый результат: "Пользователь успешно создан"

## Примечания

- Изменения в схеме БД сделаны напрямую (без Flyway миграций)
- При пересоздании БД нужно учитывать, что миграции V4 и V8 создают таблицу 
  с VARCHAR, а не с ENUM типом
- Все существующие данные сохранены (пользователь admin)

## Значения ENUM (валидные значения для role)
- `OPERATOR` (по умолчанию)
- `SUPERVISOR`
- `ADMIN`

Валидация теперь происходит на уровне приложения (Java Enum), а не на уровне БД.
