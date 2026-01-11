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
