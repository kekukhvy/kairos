-- execution_history: append-only archive of every completed run, including
-- every tick of a FIXED schedule. The source of truth for "Track" in
-- Store -> Wait -> Trigger -> Retry -> Track. A read-model/log, not a rich
-- aggregate.
CREATE TABLE execution_history (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id             UUID        NOT NULL REFERENCES tasks (id),
    schedule_id         UUID        REFERENCES schedules (id),
    source_execution_id UUID        NOT NULL,    -- executions.id this came from (repeats for FIXED)
    scheduled_for       TIMESTAMPTZ NOT NULL,    -- which tick this was
    attempt             INT         NOT NULL,
    status              VARCHAR(16) NOT NULL,    -- SUCCESS | FAILED | DEAD_LETTER
    started_at          TIMESTAMPTZ NOT NULL,
    finished_at         TIMESTAMPTZ NOT NULL,
    duration_ms         INT         NOT NULL,
    error               TEXT,                    -- nullable
    -- result: planned for the future, not part of V1 (the service's response).
    result              JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT execution_history_status_check
        CHECK (status IN ('SUCCESS', 'FAILED', 'DEAD_LETTER'))
);

-- History is read per task, most-recent-first.
CREATE INDEX idx_execution_history_task_id
    ON execution_history (task_id, finished_at DESC);

CREATE INDEX idx_execution_history_schedule_id ON execution_history (schedule_id);
CREATE INDEX idx_execution_history_source      ON execution_history (source_execution_id);
