-- A task's (service, name) pair is its human-readable identity; service is
-- immutable. Only live (not soft-deleted) tasks are covered, so a deleted
-- task's name is freed for reuse (partial index on deleted_at IS NULL).
CREATE UNIQUE INDEX idx_tasks_service_name_unique
    ON tasks (service, name)
    WHERE deleted_at IS NULL;
