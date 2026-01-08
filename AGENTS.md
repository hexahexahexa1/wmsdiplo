# AGENTS.md - Development Guidelines for WMSDIPL Project

## Overview
This document provides essential development guidelines for agentic coding assistants working on the WMSDIPL (Warehouse Management System) project. WMSDIPL is a multi-module Java 17 application with Spring Boot REST APIs, import services, and JavaFX desktop clients.

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

## Code Style Guidelines

### Language & Framework
- **Java Version**: 17 (configured in build.gradle toolchains)
- **Frameworks**: Spring Boot 3.x, JavaFX (desktop client)
- **Testing**: JUnit 5 (configured via `useJUnitPlatform()`)
- **Database**: PostgreSQL with Flyway migrations
- **Build Tool**: Gradle 8+

### Package Structure
Follow the established package naming convention:
```
com.wmsdipl.{module}.{layer}
```

Examples:
- `com.wmsdipl.core.service` - Core business services
- `com.wmsdipl.core.web` - REST controllers
- `com.wmsdipl.core.domain` - JPA entities
- `com.wmsdipl.desktop.model` - JavaFX models
- `com.wmsdipl.imports.service` - Import processing services

### Imports & Dependencies
- Group imports by package hierarchy
- Separate java.* imports from third-party imports
- Use constructor injection for Spring services (preferred over @Autowired)
- Leverage Lombok for boilerplate code (@Data, @AllArgsConstructor, etc.)

Example import order:
```java
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wmsdipl.core.domain.User;
import com.wmsdipl.core.repository.UserRepository;
```

### Naming Conventions

#### Classes & Interfaces
- PascalCase for all class names
- Service classes end with `Service`
- Repository interfaces end with `Repository`
- Controller classes end with `Controller`
- Strategy pattern classes end with `Strategy`

#### Methods
- camelCase for all methods
- Use descriptive names: `findById()`, `createUser()`, `validateInput()`
- Boolean methods: `isActive()`, `hasPermission()`, `canProcess()`
- Action methods: `save()`, `delete()`, `update()`, `process()`

#### Variables & Fields
- camelCase for instance variables and parameters
- Prefix boolean fields with `is` when appropriate
- Use descriptive names: `userRepository`, `createdAt`, `isActive`

#### Constants
- UPPER_SNAKE_CASE for constants
- Group related constants in interfaces or enums

### Error Handling
- Use `IllegalArgumentException` for invalid input parameters
- Use `IllegalStateException` for invalid object states
- Use `Optional<T>` for nullable return values instead of null
- Prefer checked exceptions for recoverable errors
- Use runtime exceptions for programming errors

Example:
```java
public User findById(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
}

public Optional<Location> determineLocation(Long palletId) {
    // Return Optional instead of null
}
```

### Transaction Management
- Use `@Transactional` at service method level
- Use `@Transactional(readOnly = true)` for read-only operations
- Prefer class-level `@Transactional` for service classes

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
- Return consistent response formats
- Use meaningful URL paths: `/api/receipts`, `/api/tasks`
- Handle validation errors gracefully

### JavaFX (Desktop Client)
- Use FXML for view definitions
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

## Copilot Instructions Integration
The `.github/copilot-instructions.md` contains general project setup guidelines that should be followed when initializing new development environments or onboarding team members.

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

## Development Workflow
1. Pull latest changes and ensure clean build
2. Create feature branch for new work
3. Write tests first (TDD approach)
4. Implement functionality with proper error handling
5. Run full test suite and verify build
6. Commit with descriptive messages
7. Create pull request for review

## Common Pitfalls to Avoid
- Don't return null from methods - use Optional<T>
- Don't catch generic Exception - be specific
- Don't hardcode configuration values
- Don't skip input validation
- Don't forget transaction boundaries for data modifications
- Don't mix UI and business logic in JavaFX controllers