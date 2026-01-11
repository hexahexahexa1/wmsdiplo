# Planning Documents

This directory contains detailed planning documents for new features and significant changes to the WMSDIPL system.

## Purpose

Planning documents help ensure:
- All requirements are clearly understood before implementation
- Edge cases and error scenarios are identified early
- Architecture decisions are documented and justified
- Testing strategy is defined upfront
- Stakeholders can review and approve plans before code is written

## When to Create a Planning Document

Create a planning document when:
- Adding a new feature that affects multiple modules
- Changing domain model (new entities or significant field changes)
- Modifying API contracts (new endpoints or DTO changes)
- Implementing complex business workflows
- Making changes that could impact existing functionality
- Refactoring that affects architecture

## Planning Mode Protocol

When an AI assistant enters Planning Mode:

1. **Interview Phase**: Ask comprehensive questions across 10 categories:
   - Functional Requirements
   - Data & Domain Model
   - API & Integration
   - Business Logic & Workflows
   - Architecture & Module Boundaries
   - Error Handling & Edge Cases
   - Testing Strategy
   - Performance & Scalability
   - Security & Authorization
   - Backwards Compatibility

2. **Documentation Phase**: Create a detailed plan using `TEMPLATE.md`

3. **Approval Phase**: Get explicit approval before starting implementation

4. **Implementation Phase**: Code only after plan is approved

## File Naming Convention

```
FEATURE-{short-description}-{YYYY-MM-DD}.md
```

Examples:
- `FEATURE-damaged-goods-tracking-2026-01-10.md`
- `FEATURE-multi-warehouse-support-2026-01-15.md`
- `REFACTOR-service-layer-restructure-2026-01-20.md`

## Document Lifecycle

1. **Draft**: Initial planning document created, gathering requirements
2. **Approved**: Stakeholder approved, ready for implementation
3. **In Progress**: Implementation ongoing
4. **Completed**: Feature implemented, tested, and deployed

## Template

Use `TEMPLATE.md` as the starting point for all planning documents. It includes:

- Business goals and user stories
- Functional requirements and edge cases
- Technical design (entities, APIs, services)
- Error handling strategy
- Testing approach
- Implementation checklist
- Risk analysis
- Performance and security considerations
- Rollback plan
- Decision log

## Best Practices

1. **Be Specific**: Avoid vague descriptions. Include concrete examples.
2. **Ask Questions**: If unsure, document the question in "Open Questions" section.
3. **Consider Alternatives**: Document why certain approaches were chosen over others.
4. **Think About Failures**: Plan for what happens when things go wrong.
5. **Update as Needed**: If scope changes during implementation, update the plan.
6. **Reference Code**: Link to similar existing implementations when relevant.

## Example Planning Documents

See past examples in this directory for reference on how to structure detailed plans.
