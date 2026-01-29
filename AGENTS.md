# AGENTS.md - Development Guidelines

## Project Overview
WMSDIPL is a multi-module Java 17 application with Spring Boot 3, using Gradle.
- **core-api**: Spring Boot REST API + PostgreSQL
- **import-service**: XML import processing service
- **desktop-client**: JavaFX desktop application
- **shared-contracts**: Shared DTOs (Java Records)

## Quick Reference Commands

### Build & Run
```bash
# Build
gradle build                           # All modules
gradle :core-api:build                # Specific module

# Run
gradle :core-api:bootRun              # Port 8080
gradle :import-service:bootRun        # Port 8090
gradle :desktop-client:run            # JavaFX Client

# Database
docker compose up -d postgres
```

### Testing
```bash
gradle test                           # All tests
gradle :core-api:test                 # Module tests
gradle :core-api:test --tests "*ReceiptServiceTest"              # Single Class
gradle :core-api:test --tests "*ReceiptMapperTest.shouldMap*"    # Single Method
```

## Code Style & Conventions

### Standards
- **Java 17**: Use Records for DTOs, `var` where clear, Stream API.
- **Imports**: Group `java.*`, `javax.*`, 3rd party, `com.wmsdipl.*`.
- **Naming**: `PascalCase` classes, `camelCase` methods/vars, `UPPER_SNAKE` constants.
- **Tests**: BDD style `shouldReturnX_WhenY`.
- **Lombok**: `@Data`, `@Builder`, `@AllArgsConstructor` to reduce boilerplate.

### Architecture Rules (CRITICAL)
1.  **Services**:
    *   Must return **Entities**, NEVER DTOs.
    *   Must use **Constructor Injection** (No `@Autowired` fields).
    *   Must be `@Transactional` (use `readOnly=true` for queries).
2.  **Controllers**:
    *   Must use **Mappers** to convert DTO ↔ Entity.
    *   Delegate all logic to Services.
3.  **DTOs**: Defined in `shared-contracts`. Immutable Records preferred.
4.  **Database**:
    *   Use `Optional<T>` instead of `null`.
    *   Enums must be `@Enumerated(EnumType.STRING)`.
5.  **Error Handling**:
    *   `ResponseStatusException` for HTTP errors.
    *   Specific exceptions (`IllegalArgumentException`) over generic `Exception`.

### Logging
- Use SLF4J: `log.info("Processing receipt: {}", id);` (No string concatenation).

## Planning Mode Protocol
**CRITICAL**: For new features, refactoring, or schema changes, you **MUST** enter Planning Mode.

1.  **Analyze & Interrogate**: Do not just accept the request. **Proactively ask 3-5 deep clarifying questions** to ensure maximum coverage and understanding. Focus on:
    *   **Edge Cases**: "What should happen if X fails?", "How do we handle concurrent access?"
    *   **User Experience**: "Should we show a loading state?", "Is a confirmation dialog needed?"
    *   **Data Integrity**: "Does this affect existing records?", "Do we need a migration?"
    *   **Security**: "Who is allowed to perform this action?"
    *   **Testing**: "What are the critical success scenarios?", "What are the failure modes?"
2.  **Document**: Create `docs/planning/FEATURE-{name}.md` using the template:
    *   Overview (Goal, User Story)
    *   Technical Design (Modules, Entities, APIs)
    *   Step-by-step Implementation Plan
    *   Test Strategy (Unit, Integration, UI)
3.  **Approval**: specific user approval of the plan is **REQUIRED** before writing code.

## Environment
- **DB Credentials**: `admin` / `admin` (Default).
- **Config**: `application.yml` + Env Vars.
- **Copilot**: See `.github/copilot-instructions.md`.
