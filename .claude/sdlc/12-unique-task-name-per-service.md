# Sync-agent run markers — 12-unique-task-name-per-service

- test-author | 2026-07-28 | ran: TDD-driven via tdd-implementer — unit (CreateTaskUseCaseTest, UpdateTaskUseCaseTest), API (TaskApiTest 409/200/201), repo IT (JooqTaskRepositoryIT existsByServiceAndName), admin component tests (TaskFormTest, TaskStepTest, FieldValidationTest) | files: 12
- javadoc-writer | 2026-07-28 | ran: covered inline — new public types (TaskNameAlreadyExistsException, TaskUniqueness) and port method (existsByServiceAndName) carry Javadoc | files: 3
- logging-instrumenter | 2026-07-28 | ran: SLF4J across task use-cases (INFO create/update/start-stop/delete, WARN on duplicate/rename rejections) + DEBUG on admin duplicate-flag in TaskForm/TaskStep | files: 6
- spec-keeper | 2026-07-28 | ran: doc/database.md (V8 partial unique index on tasks), doc/specification.md (task identity invariant, existsByServiceAndName port, TaskNameAlreadyExistsException → 409) | files: 2
- user-docs-writer | 2026-07-28 | ran: doc/usage/api.md + getting-started.md (409 on POST/PUT duplicate (service,name), soft-delete frees name) | files: 2

## Notes
- spec-keeper + user-docs-writer were initially SKIPPED during /implement and caught by the pre-PR /sdlc-check gate (BLOCK), then run to close the gap.
- logging-instrumenter initially covered only the backend use-cases by hand; the gate flagged the admin side (TaskForm/TaskStep duplicate rejection unlogged), closed with DEBUG logs.
- javadoc-writer: no separate run needed — the tdd-implementer wrote Javadoc on the new public types as it went; gate confirmed no undocumented public API remained.
