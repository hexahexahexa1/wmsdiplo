# AGENTS.md - Development Guidelines for WMSDIPL Project

## Overview
This document provides essential development guidelines for agentic coding assistants working on the WMSDIPL (Warehouse Management System) project. WMSDIPL is a multi-module Java 17 application with Spring Boot REST APIs, import services, and JavaFX desktop clients.

**Note**: The original comprehensive AGENTS.md is preserved below. This top section provides quick reference for common operations.

## Quick Reference (150-line Summary)

### Build & Test Commands
```bash
# Build
gradle build                           # Build all modules
gradle :core-api:build                # Build specific module
gradle clean build                    # Clean rebuild

# Test
gradle test                           # All tests
gradle :core-api:test                 # Module tests
gradle :core-api:test --tests "*ReceiptServiceTest"              # Single test class
gradle :core-api:test --tests "*TaskServiceTest.shouldAssign*"   # Single test method

# Run
gradle :core-api:bootRun              # Start API (port 8080)
gradle :import-service:bootRun        # Start import service
gradle :desktop-client:run            # Start JavaFX client

# Database
docker compose up -d postgres         # Start PostgreSQL
docker compose down                   # Stop containers

# Verification
gradle check                          # Run all checks
gradle compileJava                    # Compile only
```

### Project Structure
```
WMSDIPL/
├── shared-contracts/       # DTOs, request/response objects (Java records)
├── core-api/              # Spring Boot REST API + PostgreSQL
│   ├── domain/            # JPA entities, enums
│   ├── repository/        # Spring Data JPA repositories
│   ├── service/           # Business logic (works with entities, NOT DTOs)
│   │   ├── workflow/      # Multi-step workflow orchestration
│   │   └── putaway/       # Strategy pattern implementations
│   ├── mapper/            # DTO ↔ Entity conversion
│   ├── web/               # REST controllers (uses mappers + services)
│   └── config/            # Spring configuration
├── import-service/        # XML import processing service
└── desktop-client/        # JavaFX desktop application
```

### Code Style Essentials

#### Import Order
```java
import java.time.LocalDateTime;        // Java standard library
import java.util.List;

import org.springframework.stereotype.Service;  // Third-party libraries

import com.wmsdipl.contracts.dto.ReceiptDto;    // Project packages
import com.wmsdipl.core.domain.Receipt;
```

#### Naming Conventions
- **Classes**: `PascalCase` (e.g., `ReceiptService`, `TaskController`, `PutawayStrategy`)
- **Methods**: `camelCase` (e.g., `findById()`, `createReceipt()`, `isActive()`)
- **Variables**: `camelCase` (e.g., `userRepository`, `createdAt`, `isProcessed`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
- **Test methods**: BDD style (e.g., `shouldReturnReceipt_WhenValidId()`)

#### Layered Architecture Rules
```java
// ❌ WRONG: Service depends on DTOs
@Service
public class ReceiptService {
    public ReceiptDto create(CreateReceiptRequest dto) { ... }  // NO!
}

// ✅ CORRECT: Service works with entities, mapper handles DTOs
@Service
public class ReceiptService {
    public Receipt create(Receipt receipt) { ... }  // Returns entity
}

@RestController
public class ReceiptController {
    private final ReceiptService service;
    private final ReceiptMapper mapper;
    
    @PostMapping
    public ReceiptDto create(@Valid @RequestBody CreateReceiptRequest request) {
        Receipt entity = mapper.toEntity(request);    // DTO → Entity
        Receipt saved = service.create(entity);
        return mapper.toDto(saved);                   // Entity → DTO
    }
}
```

#### Error Handling
```java
// Use Optional instead of null
public Optional<Location> findLocation(Long id) { ... }

// Throw specific exceptions
throw new IllegalArgumentException("Invalid input: " + value);
throw new IllegalStateException("Receipt already confirmed");
throw new ResponseStatusException(NOT_FOUND, "Receipt not found: " + id);
throw new ResponseStatusException(CONFLICT, "Duplicate barcode");
```

#### Transaction Management
```java
@Service
@Transactional  // Class-level for all methods
public class ReceiptService {
    
    @Transactional(readOnly = true)  // Read-only optimization
    public List<Receipt> findAll() { ... }
    
    // Write methods use default @Transactional
    public Receipt save(Receipt receipt) { ... }
}
```

#### Dependency Injection
```java
// ✅ CORRECT: Constructor injection
@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserService userService;
    
    public TaskService(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }
}

// ❌ WRONG: Field injection
@Autowired
private TaskRepository taskRepository;  // Don't use @Autowired
```

#### Code Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters max
- **Braces**: Opening brace on same line
- **Blank lines**: Between import groups, between methods

#### JPA & Database
```java
@Entity
@Table(name = "receipts")
public class Receipt {
    @Enumerated(EnumType.STRING)  // ✅ Use STRING, never ORDINAL
    private ReceiptStatus status;
    
    @OneToMany(mappedBy = "receipt")
    private List<ReceiptLine> lines;
}
```

#### Lombok Annotations
```java
@Data                          // Generates getters, setters, toString, equals, hashCode
@AllArgsConstructor           // All-args constructor
@NoArgsConstructor            // No-args constructor (required for JPA)
@Builder                      // Builder pattern
public class Receipt { ... }
```

#### Logging
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReceiptService {
    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);
    
    public void process(Long id) {
        log.info("Processing receipt: {}", id);  // ✅ Parameterized
        log.error("Failed to process: {}", id, exception);
        // ❌ Don't: log.info("Processing: " + id);  // String concatenation
    }
}
```

### Critical Rules (Must Follow)
1. **Services NEVER depend on DTOs** - use domain entities only
2. **Controllers use Mappers** - convert DTOs ↔ Entities
3. **Return `Optional<T>`** instead of null
4. **Constructor injection** - not `@Autowired` fields
5. **`@Transactional`** on service methods, `readOnly = true` for queries
6. **`@Enumerated(EnumType.STRING)`** for all enums (never ORDINAL)
7. **Test naming**: BDD style like `shouldReturnUser_WhenValidId()`
8. **Planning Mode**: Ask requirements questions BEFORE coding new features

### Common Pitfalls to Avoid
- ❌ Returning `null` (use `Optional<T>`)
- ❌ Services depending on DTOs (use entities)
- ❌ Catching generic `Exception` (be specific)
- ❌ Hardcoded values (use configuration)
- ❌ `@Autowired` fields (use constructor injection)
- ❌ Mixing UI and business logic in controllers
- ❌ Duplicating DTOs across modules (use shared-contracts)
- ❌ Creating circular service dependencies

### Testing Examples
```java
@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {
    @Mock private ReceiptRepository receiptRepository;
    @InjectMocks private ReceiptService receiptService;
    
    @Test
    void shouldReturnReceipt_WhenValidId() {
        // Given
        Receipt receipt = new Receipt();
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        
        // When
        Receipt result = receiptService.findById(1L);
        
        // Then
        assertNotNull(result);
        verify(receiptRepository).findById(1L);
    }
}
```

### Environment Variables
- **Database**: PostgreSQL via Docker Compose
- **Ports**: Core API (8080), Import Service (8090)
- **Default User**: `admin` / `admin`
- **Config**: `application.yml` + environment variables for secrets

### Copilot Integration
See `.github/copilot-instructions.md` for project setup guidelines when initializing new development environments.

---

## Planning Mode: Requirements Gathering & Analysis

**CRITICAL**: When a user requests a new feature, refactoring, or significant change, you MUST enter "Planning Mode" BEFORE writing any code.

### Planning Mode Protocol

1. **Initial Understanding**
   - Acknowledge the request
   - State that you're entering Planning Mode
   - Ask for clarification on the high-level goal

2. **Deep Dive Interview** (Ask ALL relevant questions from the categories below)

   **A. Functional Requirements**
   - What is the exact business goal this feature achieves?
   - Who are the end users (warehouse operators, admins, API consumers)?
   - What are the inputs and expected outputs?
   - What are the success criteria?
   - Are there any acceptance criteria or user stories?
   - What happens in edge cases (empty data, missing fields, etc.)?

   **B. Data & Domain Model**
   - Which existing domain entities are involved (Receipt, Task, Pallet, Location, SKU, etc.)?
   - Do we need new entities or modifications to existing ones?
   - What are the relationships between entities (one-to-many, many-to-many)?
   - What data needs to be persisted vs. calculated?
   - Are there any database constraints (unique, not null, foreign keys)?
   - Do we need Flyway migrations for schema changes?

   **C. API & Integration**
   - Does this feature expose new REST endpoints?
   - What are the HTTP methods and URL paths?
   - What are the request/response DTOs?
   - Are there validation requirements (NotBlank, Min, Max, etc.)?
   - Does this integrate with existing APIs (receiving, placement, putaway)?
   - Will JavaFX desktop client need changes too?
   - Does import-service need to handle this data?

   **D. Business Logic & Workflows**
   - Are there state transitions (DRAFT → CONFIRMED → IN_PROGRESS)?
   - What validations are required before state changes?
   - Are there dependencies on other services/workflows?
   - How does this affect existing workflows (receiving, placement, putaway)?
   - Are there any rollback/compensation scenarios?
   - What happens on failure (discard, retry, compensate)?

   **E. Architecture & Module Boundaries**
   - Which module(s) are affected (core-api, shared-contracts, desktop-client, import-service)?
   - Do we need new DTOs in shared-contracts?
   - Do we need new mappers for DTO ↔ Entity conversion?
   - Will this introduce new service dependencies?
   - Are we following the layered architecture (Controller → Mapper → Service → Repository)?
   - Could this create circular dependencies?

   **F. Error Handling & Edge Cases**
   - What exceptions should be thrown (IllegalArgumentException, IllegalStateException, ResponseStatusException)?
   - What HTTP status codes for errors (400, 404, 409, 500)?
   - How to handle concurrent modifications?
   - What if referenced entities don't exist (Receipt not found, SKU missing)?
   - What if data is in an invalid state for the operation?
   - How to handle partial failures in multi-step workflows?

   **G. Testing Strategy**
   - What unit tests are needed (service layer, mappers)?
   - What controller tests are needed (MockMvc)?
   - Do we need integration tests (Testcontainers)?
   - What are the key test scenarios (happy path, validation errors, edge cases)?
   - How to test state transitions and workflows?
   - Do we need to test rollback/compensation logic?

   **H. Performance & Scalability**
   - Will this operate on large datasets (need pagination)?
   - Are there N+1 query risks (eager/lazy loading)?
   - Do we need database indexes for new queries?
   - Should we cache any computed results?
   - Are there concurrency concerns (optimistic/pessimistic locking)?

   **I. Security & Authorization**
   - Does this require authentication/authorization?
   - Are there role-based access controls (operator, supervisor, admin)?
   - Do we need to audit this operation (who did what when)?
   - Any sensitive data that shouldn't be logged?

   **J. Backwards Compatibility**
   - Will this break existing API contracts?
   - Do existing clients need updates?
   - Are there database migrations that could fail on existing data?
   - Do we need a deprecation plan for old endpoints?

3. **Document the Plan**
   - After gathering all answers, create a file: `docs/planning/FEATURE-{name}-{YYYY-MM-DD}.md`
   - Use the template provided below
   - Get explicit user confirmation before proceeding

4. **Implementation Phase**
   - Only start coding AFTER the plan is documented and approved
   - Reference the plan file in commit messages
   - Update the plan if scope changes during implementation

### Planning Document Template

When creating a planning document in `docs/planning/`, use this structure:

```markdown
# Feature Plan: {Feature Name}

**Date**: {YYYY-MM-DD}  
**Author**: AI Assistant  
**Status**: Draft | Approved | In Progress | Completed  
**Related Issues**: #{issue number if applicable}

## 1. Overview

### Business Goal
{What problem does this solve? What value does it provide?}

### User Story
As a {user type}, I want {goal} so that {benefit}.

### Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

## 2. Functional Requirements

### Inputs
- {List all input parameters, their types, and validation rules}

### Outputs
- {Expected outputs, response formats}

### Business Rules
1. {Rule 1}
2. {Rule 2}

### Edge Cases
- **Scenario**: {edge case description}
  - **Handling**: {how to handle it}

## 3. Technical Design

### Affected Modules
- [ ] core-api
- [ ] shared-contracts
- [ ] import-service
- [ ] desktop-client

### Domain Model Changes

#### New Entities
- `EntityName`: {description, fields, relationships}

#### Modified Entities
- `ExistingEntity`: 
  - Added fields: {list}
  - Modified fields: {list}
  - New relationships: {list}

#### Database Migrations
```sql
-- V{version}__description.sql
-- {migration script outline}
```

### API Design

#### New Endpoints
```
POST /api/{resource}
GET /api/{resource}/{id}
PUT /api/{resource}/{id}
DELETE /api/{resource}/{id}
```

#### Request DTOs
```java
// CreateXxxRequest
{
  field1: type,
  field2: type
}
```

#### Response DTOs
```java
// XxxDto
{
  id: Long,
  field1: type,
  field2: type
}
```

### Service Layer Design

#### New Services
- `XxxService`: {responsibilities}

#### Modified Services
- `ExistingService`: {what changes}

#### Service Dependencies
```
XxxService
  ├── XxxRepository
  ├── RelatedService1
  └── RelatedService2
```

### Workflow & State Machine

```
[State1] --event--> [State2] --event--> [State3]
```

**State Transitions**:
- State1 → State2: {conditions, validations}
- State2 → State3: {conditions, validations}

## 4. Error Handling

### Exception Mapping
- `IllegalArgumentException`: {when}
- `IllegalStateException`: {when}
- `ResponseStatusException(404)`: {when}
- `ResponseStatusException(409)`: {when}

### Error Response Format
```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "..."
}
```

## 5. Testing Strategy

### Unit Tests
- [ ] `XxxServiceTest`: {test scenarios}
- [ ] `XxxMapperTest`: {test scenarios}

### Integration Tests
- [ ] `XxxControllerTest`: {MockMvc scenarios}
- [ ] `XxxWorkflowIntegrationTest`: {end-to-end scenarios}

### Test Data
- {Required test fixtures, sample data}

## 6. Implementation Checklist

### Phase 1: Data Model
- [ ] Create/modify domain entities
- [ ] Write Flyway migration scripts
- [ ] Update repository interfaces

### Phase 2: Service Layer
- [ ] Create DTOs in shared-contracts
- [ ] Implement service business logic
- [ ] Write service unit tests
- [ ] Create mappers

### Phase 3: API Layer
- [ ] Implement REST controllers
- [ ] Add Swagger/OpenAPI annotations
- [ ] Write controller tests (MockMvc)

### Phase 4: Integration
- [ ] Update desktop client (if needed)
- [ ] Update import service (if needed)
- [ ] Write integration tests

### Phase 5: Documentation
- [ ] Update API documentation
- [ ] Update ER diagram (docs/er-diagram.mmd)
- [ ] Update AGENTS.md if architecture changes

## 7. Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| {risk description} | High/Med/Low | High/Med/Low | {mitigation strategy} |

## 8. Performance Considerations

- **Database Indexes**: {which columns need indexes}
- **Query Optimization**: {N+1 prevention, eager loading strategy}
- **Caching**: {what to cache, TTL}
- **Pagination**: {if dealing with large datasets}

## 9. Security Considerations

- **Authorization**: {who can access this feature}
- **Validation**: {input validation rules}
- **Audit**: {what to log, who did what}

## 10. Rollback Plan

- **Database Rollback**: {how to revert schema changes}
- **Code Rollback**: {feature flags, backward compatibility}
- **Data Migration**: {if data needs to be migrated back}

## 11. Questions & Decisions Log

### Open Questions
- [ ] Question 1: {answer once resolved}
- [ ] Question 2: {answer once resolved}

### Decisions Made
- **Decision**: {what was decided}
  - **Rationale**: {why}
  - **Alternatives Considered**: {what else was considered}
  - **Date**: {YYYY-MM-DD}

## 12. Success Metrics

- {How will we measure success?}
- {What metrics to track?}

## 13. Timeline Estimate

- **Planning**: {X hours}
- **Implementation**: {X hours}
- **Testing**: {X hours}
- **Documentation**: {X hours}
- **Total**: {X hours}

## 14. References

- Related documentation: {links}
- Similar features: {links to similar code}
- External resources: {links}
```

### Example: Entering Planning Mode

**User**: "I need to add a feature to track damaged goods during receiving"

**Assistant Response**:
```
I'm entering Planning Mode to fully understand this requirement before implementation.

Let me ask some clarifying questions to ensure we cover all aspects:

**Functional Requirements**:
1. Should damaged goods be recorded at the scan level or as a separate entity?
2. Do we need to capture the extent of damage (damaged count vs. total count)?
3. Should damaged goods affect the receipt qty (reduce expected qty)?
4. Can operators mark goods as damaged after initial scan, or only during scan?
5. What happens to damaged goods - are they discarded, returned, or moved to a special location?

**Data & Domain Model**:
6. Should we add a 'damaged' flag to Scan entity or create a new DamagedGoods entity?
7. Do we need to track damage reasons/types (water damage, physical damage, expired)?
8. Should damage be linked to specific pallets or just general receipt lines?
9. Do we need photos or evidence documentation for damage claims?

**Workflows**:
10. Should recording damage create a discrepancy automatically?
11. Does damage recording require supervisor approval?
12. Should damaged goods be excluded from placement tasks?
13. What receipt status transitions are affected (can we complete receiving with damaged goods)?

... (continue with all relevant questions from the protocol)

Once I have your answers, I'll create a detailed plan in docs/planning/FEATURE-damaged-goods-tracking-{date}.md for your review before starting implementation.
```

### Enforcement Rules

1. **NO CODE WITHOUT A PLAN** for features/changes affecting:
   - Domain model (new entities, fields, relationships)
   - API contracts (new endpoints, DTO changes)
   - Business workflows (state transitions, validations)
   - Multi-module changes

2. **ALWAYS CREATE** `docs/planning/` file before implementation

3. **GET APPROVAL** before proceeding to implementation phase

4. **UPDATE PLAN** if scope changes during implementation

This Planning Mode ensures thorough analysis, prevents rework, and maintains high code quality by catching issues before implementation.

## Project Structure
- `shared-contracts/` - Shared DTOs and API contracts used across all modules
- `core-api/` - Spring Boot REST API with PostgreSQL and Flyway migrations
- `import-service/` - Spring Boot service for XML import processing  
- `desktop-client/` - JavaFX desktop application
- `docs/` - Documentation and ER diagrams

## Build System & Commands

### Full Project Build
```bash
gradle build          # Compile, test, and package all modules
gradle clean build    # Clean and rebuild
```

### Module-Specific Builds
```bash
gradle :shared-contracts:build     # Build shared contracts module
gradle :core-api:build             # Build core API module
gradle :import-service:build       # Build import service module
gradle :desktop-client:build       # Build desktop client module
```

### Running Applications
```bash
gradle :core-api:bootRun           # Start core API server (port 8080)
gradle :import-service:bootRun     # Start import service
gradle :desktop-client:run         # Start JavaFX desktop client
```

### Testing
```bash
gradle test                     # Run all tests across modules
gradle :core-api:test          # Run core API tests only
gradle :import-service:test    # Run import service tests
gradle :desktop-client:test    # Run desktop client tests
```

### Running Single Tests
Use the `--tests` flag with wildcards to run specific test classes or methods:
```bash
gradle :core-api:test --tests "*TestClassName"
gradle :core-api:test --tests "*TestClassName.testMethodName"
```

Examples:
```bash
gradle :core-api:test --tests "*ReceiptServiceTest"
gradle :core-api:test --tests "*TaskServiceTest.shouldAssignTask_WhenValidId"
```

### Database Setup
```bash
docker compose up -d postgres     # Start PostgreSQL container
docker compose down               # Stop all containers
```

### Code Quality & Verification
```bash
gradle check           # Run all verification tasks (tests, code quality)
gradle compileJava     # Compile main source only
gradle compileTestJava # Compile test source only
```

## Code Style Guidelines

### Language & Framework
- **Java Version**: 17 (configured in build.gradle toolchains)
- **Frameworks**: Spring Boot 3.x, JavaFX (desktop client)
- **Testing**: JUnit 5 (configured via `useJUnitPlatform()`)
- **Database**: PostgreSQL with Flyway migrations
- **Build Tool**: Gradle 8+

### Module Architecture

#### shared-contracts Module
Contains all DTOs and request/response objects shared across modules:
- Located in `com.wmsdipl.contracts.dto`
- Uses Java records for immutability
- No business logic - pure data transfer objects
- Depends only on Jakarta Validation API

#### core-api Module Structure
```
com.wmsdipl.core
├── domain/          - JPA entities and enums
├── repository/      - Spring Data JPA repositories
├── service/         - Business logic services
│   ├── workflow/    - Workflow orchestration services (ReceivingWorkflowService, PlacementWorkflowService)
│   └── putaway/     - Putaway strategy implementations + support services
│       ├── *Strategy.java           - Strategy implementations (FIFO, FEFO, etc.)
│       ├── LocationSelectionService - Location selection logic
│       ├── PutawayContextBuilder    - Context building from pallet data
│       └── StrategyRegistry         - Strategy pattern registry
├── mapper/          - DTO ↔ Domain mappers
├── web/             - REST controllers (consolidated from api/ and web/)
└── config/          - Spring configuration classes
```

### Package Structure
Follow the established package naming convention:
```
com.wmsdipl.{module}.{layer}
```

Examples:
- `com.wmsdipl.core.service` - Core business services
- `com.wmsdipl.core.service.workflow` - Workflow orchestration services
- `com.wmsdipl.core.web` - REST controllers
- `com.wmsdipl.core.domain` - JPA entities
- `com.wmsdipl.core.mapper` - DTO-Domain mappers
- `com.wmsdipl.contracts.dto` - Shared DTOs
- `com.wmsdipl.desktop.model` - JavaFX models
- `com.wmsdipl.imports.service` - Import processing services

### Imports & Dependencies
- Group imports by package hierarchy
- Separate java.* imports from third-party imports
- Use constructor injection for Spring services (preferred over @Autowired)
- Leverage Lombok for boilerplate code (@Data, @AllArgsConstructor, etc.)
- **Controllers depend on**: Services (via constructor injection), Mappers
- **Services depend on**: Repositories, Domain entities, other Services
- **Services do NOT depend on**: DTOs (use Mappers instead)
- **All modules depend on**: shared-contracts for DTOs

Example import order:
```java
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wmsdipl.contracts.dto.ReceiptDto;
import com.wmsdipl.core.domain.Receipt;
import com.wmsdipl.core.mapper.ReceiptMapper;
import com.wmsdipl.core.repository.ReceiptRepository;
```

### Naming Conventions

#### Classes & Interfaces
- PascalCase for all class names
- Service classes end with `Service`
- Repository interfaces end with `Repository`
- Controller classes end with `Controller`
- Strategy pattern classes end with `Strategy`
- Mapper classes end with `Mapper`

#### Methods
- camelCase for all methods
- Use descriptive names: `findById()`, `createUser()`, `validateInput()`
- Boolean methods: `isActive()`, `hasPermission()`, `canProcess()`
- Action methods: `save()`, `delete()`, `update()`, `process()`
- Mapper methods: `toDto()`, `toEntity()`, `updateEntity()`

#### Variables & Fields
- camelCase for instance variables and parameters
- Prefix boolean fields with `is` when appropriate
- Use descriptive names: `userRepository`, `createdAt`, `isActive`

#### Constants
- UPPER_SNAKE_CASE for constants
- Group related constants in interfaces or enums

### Layering and Separation of Concerns

#### Controller Layer (`com.wmsdipl.core.web`)
- Handle HTTP requests/responses
- Use mappers to convert between DTOs and domain objects
- Delegate business logic to services
- Use `@RestController` and `@RequestMapping`
- Return DTOs, not domain entities

#### Mapper Layer (`com.wmsdipl.core.mapper`)
- Convert between DTOs and domain entities
- Annotated with `@Component`
- Methods: `toDto()`, `toEntity()`, `updateEntity()`
- No business logic - pure transformation
- Injected into controllers

#### Service Layer (`com.wmsdipl.core.service`)
- Contains all business logic
- Works with domain entities, NOT DTOs
- Annotated with `@Service` and `@Transactional`
- Depends on repositories and other services
- Returns domain entities

#### Workflow Services (`com.wmsdipl.core.service.workflow`)
- Orchestrate complex multi-step workflows
- Examples: `ReceivingWorkflowService`, `PlacementWorkflowService`
- Coordinate multiple repositories and services
- Handle state transitions

#### Repository Layer (`com.wmsdipl.core.repository`)
- Extend `JpaRepository`
- Minimal custom query methods
- Use Spring Data JPA derived queries
- Return domain entities

### Error Handling
- Use `IllegalArgumentException` for invalid input parameters
- Use `IllegalStateException` for invalid object states
- Use `ResponseStatusException` in services for HTTP errors
- Use `Optional<T>` for nullable return values instead of null
- Prefer checked exceptions for recoverable errors
- Use runtime exceptions for programming errors

Example:
```java
public Receipt findById(Long id) {
    return receiptRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Receipt not found: " + id));
}

public Optional<Location> determineLocation(Long palletId) {
    // Return Optional instead of null
}
```

### Transaction Management
- Use `@Transactional` at service method level
- Use `@Transactional(readOnly = true)` for read-only operations
- Prefer class-level `@Transactional` for service classes
- Workflow services require careful transaction boundary management

### Code Formatting
- 4-space indentation (standard Java convention)
- Max line length: 120 characters
- Opening braces on same line as method/class declaration
- Consistent spacing around operators and keywords

### Annotations & Lombok
- Use Lombok annotations to reduce boilerplate:
  - `@Data` for simple POJOs
  - `@AllArgsConstructor` + `@NoArgsConstructor` for entities
  - `@Builder` for complex object construction
- Place annotations above class/method/field declarations
- Use `@Transactional` appropriately for service methods

### Database & JPA
- Use JPA entities with proper `@Entity`, `@Table`, `@Column` annotations
- Define relationships with `@OneToMany`, `@ManyToOne`, etc.
- Use `@Enumerated(EnumType.STRING)` for enums
- Implement Flyway migrations for schema changes
- Use meaningful constraint names and indexes

### REST API Design
- Follow RESTful conventions
- Use appropriate HTTP methods (GET, POST, PUT, DELETE)
- Return consistent response formats (DTOs from shared-contracts)
- Use meaningful URL paths: `/api/receipts`, `/api/tasks`
- Handle validation errors gracefully with `@ControllerAdvice`

### JavaFX (Desktop Client)
- Use FXML for view definitions (recommended)
- Follow MVC pattern with controllers
- Use JavaFX properties for data binding
- Handle UI threading appropriately (Platform.runLater())
- Use descriptive controller method names

### Logging
- Use SLF4J logging interface
- Log levels: ERROR, WARN, INFO, DEBUG, TRACE
- Include relevant context in log messages
- Use parameterized logging to avoid string concatenation

### Security Considerations
- Validate all input parameters
- Use prepared statements (handled by Spring Data JPA)
- Implement proper authentication/authorization when required
- Avoid logging sensitive information
- Use environment variables for configuration

### Testing Guidelines
- Write unit tests for service methods
- Use `@SpringBootTest` for integration tests
- Use `@Testcontainers` for database integration tests
- Test both success and failure scenarios
- Use descriptive test method names with BDD style: `shouldReturnUser_WhenValidId()`
- Mock repositories and external dependencies

### Configuration Management
- Use `application.yml` for configuration
- Externalize sensitive data via environment variables
- Provide sensible defaults for development
- Document required environment variables in README

### Git & Version Control
- Follow conventional commit messages
- Use feature branches for development
- Ensure builds pass before committing
- Include relevant tests with new features

## Architecture Patterns

### Mapper Pattern
Mappers decouple the service layer from DTOs:
```java
@Component
public class ReceiptMapper {
    public ReceiptDto toDto(Receipt receipt) {
        return new ReceiptDto(/* map fields */);
    }
    
    public Receipt toEntity(CreateReceiptRequest request) {
        // Create entity from request DTO
    }
}
```

### Strategy Pattern
Used for putaway location determination:
```java
public interface PutawayStrategy {
    Optional<Location> selectLocation(PutawayContext context);
}

@Component("FIFO_DIRECTED")
public class FifoDirectedStrategy implements PutawayStrategy {
    // Implementation
}
```

### Workflow Pattern
Complex multi-step processes use workflow services:
```java
@Service
public class ReceivingWorkflowService {
    // Orchestrate DRAFT → CONFIRMED → RECEIVING → ACCEPTED
}
```

## Copilot Instructions Integration
The `.github/copilot-instructions.md` contains general project setup guidelines that should be followed when initializing new development environments or onboarding team members.

## Common Pitfalls to Avoid
- Don't return null from methods - use Optional<T>
- Don't catch generic Exception - be specific
- Don't hardcode configuration values
- Don't skip input validation
- Don't forget transaction boundaries for data modifications
- Don't mix UI and business logic in JavaFX controllers
- **Don't make services depend on DTOs** - use mappers
- **Don't duplicate DTOs across modules** - use shared-contracts
- Don't create circular dependencies between services

## Development Workflow
1. Pull latest changes and ensure clean build
2. Create feature branch for new work
3. Write tests first (TDD approach)
4. Implement functionality with proper error handling
5. Use mappers to convert between DTOs and entities
6. Run full test suite and verify build
7. Commit with descriptive messages
8. Create pull request for review

## Recent Restructuring (January 2026)
The project was recently restructured to improve modularity and reduce service coupling:
- ✅ Created `shared-contracts` module for DTOs
- ✅ Consolidated `api` and `web` controller packages into single `web` package
- ✅ Introduced mapper layer to decouple services from DTOs
- ✅ Separated workflow orchestration into `service.workflow` subpackage
- ✅ Added Swagger/OpenAPI documentation to all REST endpoints
- ✅ Refactored PutawayService: reduced dependencies from 7 to 6 by extracting LocationSelectionService and PutawayContextBuilder
- ✅ Extracted TaskLifecycleService for task state management
- ✅ Improved ReceivingWorkflowService structure with helper methods
- ⚠️ Test coverage remains at 0% - critical priority to implement

## Performance Considerations
- Use pagination for large result sets
- Implement proper database indexing
- Cache frequently accessed data when appropriate
- Monitor memory usage in JavaFX applications
- Use streaming for large file processing in import service

## Documentation
- Maintain ER diagrams in `docs/er-diagram.mmd`
- Document API endpoints in README
- Include JavaDoc for public APIs
- Keep migration scripts well-documented
- Update this AGENTS.md when architecture changes

## Quick Reference: Key Module Boundaries
- **shared-contracts** exports DTOs → all modules depend on it
- **Controllers** → use Mappers + Services (never DTOs directly in services)
- **Services** → use Repositories + Domain entities (return entities, not DTOs)
- **Mappers** → convert between DTOs ↔ Domain entities (injected into controllers)

## Critical Rules
1. **Services must NOT depend on DTOs** - use domain entities only
2. **Controllers convert via Mappers** - `toDto()`, `toEntity()`, `updateEntity()`
3. **Return Optional\<T\>** instead of null
4. **Use constructor injection** (not @Autowired) for Spring components
5. **Test naming**: BDD style like `shouldReturnUser_WhenValidId()`
6. **Transactions**: Use `@Transactional` on service methods, `readOnly = true` for queries

## Code Formatting Standards
- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters max
- **Import order**: java.* → blank line → third-party → blank line → com.wmsdipl.*
- **Braces**: Opening brace on same line as declaration
- **Naming**: PascalCase (classes), camelCase (methods/fields), UPPER_SNAKE_CASE (constants)

## Annotations & Best Practices
- **Lombok**: Use `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Builder` to reduce boilerplate
- **JPA**: Use `@Enumerated(EnumType.STRING)` for enums (never ORDINAL)
- **Logging**: SLF4J with parameterized messages: `log.info("User {} created", userId)`
- **JavaDoc**: Required for public APIs and complex business logic

## Environment & Credentials
- **Database**: PostgreSQL via Docker (see docker-compose.yml)
- **Default user**: `admin` / `admin`
- **Ports**: Core API (8080), Import Service (8090)
- **Config**: Use `application.yml` + environment variables for secrets