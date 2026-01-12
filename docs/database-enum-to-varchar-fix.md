# Database Schema Fix: PostgreSQL ENUM → VARCHAR(32)

**Date**: 2026-01-12  
**Issue**: JPA `@Enumerated(EnumType.STRING)` incompatible with PostgreSQL ENUM types  
**Solution**: Convert all ENUM columns to VARCHAR(32)

## Problem

Spring Boot JPA with `@Enumerated(EnumType.STRING)` expects VARCHAR columns in PostgreSQL, but the database was created with PostgreSQL custom ENUM types. This caused errors like:

```
ERROR: column "status" is of type receipt_status but expression is of type character varying
```

## Root Cause

Flyway migrations (V2, V3, V4, etc.) created PostgreSQL ENUM types:
- `user_role`
- `receipt_status`
- `task_status`
- `task_type`
- `location_type`
- `location_status`
- `pallet_status`
- `movement_type`

JPA Entity classes use Java enums with `@Enumerated(EnumType.STRING)`, which maps to VARCHAR in PostgreSQL, not custom ENUM types.

## Solution Applied

### 1. Convert All ENUM Columns to VARCHAR(32)

```sql
-- users table
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(32) USING role::text;
ALTER TABLE users ALTER COLUMN role DROP DEFAULT;
DROP TYPE IF EXISTS user_role;
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'OPERATOR';

-- receipts table
ALTER TABLE receipts ALTER COLUMN status TYPE VARCHAR(32) USING status::text;
ALTER TABLE receipts ALTER COLUMN status DROP DEFAULT;
ALTER TABLE receipts ALTER COLUMN status SET DEFAULT 'DRAFT';

-- tasks table
ALTER TABLE tasks ALTER COLUMN status TYPE VARCHAR(32) USING status::text;
ALTER TABLE tasks ALTER COLUMN status DROP DEFAULT;
ALTER TABLE tasks ALTER COLUMN status SET DEFAULT 'NEW';

ALTER TABLE tasks ALTER COLUMN task_type TYPE VARCHAR(32) USING task_type::text;

-- locations table
ALTER TABLE locations ALTER COLUMN location_type TYPE VARCHAR(32) USING location_type::text;
ALTER TABLE locations ALTER COLUMN status TYPE VARCHAR(32) USING status::text;
ALTER TABLE locations ALTER COLUMN status SET DEFAULT 'AVAILABLE';

-- pallets table
ALTER TABLE pallets ALTER COLUMN status TYPE VARCHAR(32) USING status::text;
ALTER TABLE pallets ALTER COLUMN status SET DEFAULT 'DRAFT';

-- pallet_movements table
ALTER TABLE pallet_movements ALTER COLUMN movement_type TYPE VARCHAR(32) USING movement_type::text;
```

### 2. Drop All ENUM Types

```sql
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS receipt_status CASCADE;
DROP TYPE IF EXISTS task_status CASCADE;
DROP TYPE IF EXISTS task_type CASCADE;
DROP TYPE IF EXISTS location_type CASCADE;
DROP TYPE IF EXISTS location_status CASCADE;
DROP TYPE IF EXISTS pallet_status CASCADE;
DROP TYPE IF EXISTS movement_type CASCADE;
```

### 3. Verify No ENUM Types Remain

```sql
SELECT typname FROM pg_type WHERE typtype = 'e' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public');
-- Expected result: (0 rows)
```

## Affected Tables & Columns

| Table | Column | Old Type | New Type | Default Value |
|-------|--------|----------|----------|---------------|
| `users` | `role` | `user_role` | `VARCHAR(32)` | `'OPERATOR'` |
| `receipts` | `status` | `receipt_status` | `VARCHAR(32)` | `'DRAFT'` |
| `tasks` | `status` | `task_status` | `VARCHAR(32)` | `'NEW'` |
| `tasks` | `task_type` | `task_type` | `VARCHAR(32)` | *(none)* |
| `locations` | `location_type` | `location_type` | `VARCHAR(32)` | *(none)* |
| `locations` | `status` | `location_status` | `VARCHAR(32)` | `'AVAILABLE'` |
| `pallets` | `status` | `pallet_status` | `VARCHAR(32)` | `'DRAFT'` |
| `pallet_movements` | `movement_type` | `movement_type` | `VARCHAR(32)` | *(none)* |

## JPA Entity Annotations (Correct Usage)

All Java entity classes correctly use:

```java
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false, length = 32)
private ReceiptStatus status;
```

This annotation tells Hibernate to store enum values as VARCHAR strings (e.g., `"DRAFT"`, `"CONFIRMED"`), NOT as PostgreSQL ENUM types.

## Impact

- ✅ **User creation** now works (was failing on `users.role`)
- ✅ **Putaway rules display** works (was failing on enum columns)
- ✅ **XML import** works (was failing on `receipts.status`)
- ✅ **Start receiving workflow** works (was failing on `tasks.task_type`)
- ✅ All CRUD operations on entities with enums work correctly

## Data Preservation

All existing data was preserved during migration using `USING column::text` cast.

## Future Migrations

**IMPORTANT**: Future Flyway migrations must use `VARCHAR(length)` for enum columns, NOT PostgreSQL ENUM types.

### ❌ WRONG (Do NOT use):
```sql
CREATE TYPE receipt_status AS ENUM ('DRAFT', 'CONFIRMED', 'IN_PROGRESS');
CREATE TABLE receipts (
    status receipt_status NOT NULL DEFAULT 'DRAFT'
);
```

### ✅ CORRECT (Use this):
```sql
CREATE TABLE receipts (
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT'
);
```

## Validation Commands

```bash
# Check no ENUM types exist
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "SELECT typname FROM pg_type WHERE typtype = 'e';"

# Verify column types
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "\d users"
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "\d receipts"
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "\d tasks"
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "\d locations"
docker exec -i wmsdipl-postgres psql -U wmsdipl -d wmsdipl -c "\d pallets"
```

## Related Issues Fixed

1. **User Creation Error** (2026-01-12) - `users.role` ENUM → VARCHAR(32)
2. **Putaway Rules Display Error** (2026-01-12) - Multiple enum columns fixed
3. **XML Import 500 Error** (2026-01-12) - `receipts.status` ENUM → VARCHAR(32)
4. **Start Receiving 500 Error** (2026-01-12) - `tasks.task_type` ENUM → VARCHAR(32)

## References

- PostgreSQL ENUM documentation: https://www.postgresql.org/docs/current/datatype-enum.html
- JPA @Enumerated: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/enumerated
- AGENTS.md: Lines 1030-1045 (Critical Rules section)
