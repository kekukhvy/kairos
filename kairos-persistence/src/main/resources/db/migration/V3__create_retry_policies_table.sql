-- retry_policies: explicit retry steps for a task (attempt -> delay).
-- A separate table instead of a single JSON blob, so each step is
-- individually visible and editable. A retry step has no meaning outside
-- its task, hence ON DELETE CASCADE.
CREATE TABLE retry_policies (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id        UUID        NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    attempt_number INT         NOT NULL,        -- which attempt this row describes (2, 3, ...)
    delay_seconds  INT         NOT NULL,        -- delay before this attempt
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- If there's no row for the next attempt, the failure goes to DEAD_LETTER.
    CONSTRAINT retry_policies_task_attempt_unique UNIQUE (task_id, attempt_number),
    CONSTRAINT retry_policies_attempt_number_check CHECK (attempt_number > 0),
    CONSTRAINT retry_policies_delay_seconds_check  CHECK (delay_seconds >= 0)
);

CREATE INDEX idx_retry_policies_task_id ON retry_policies (task_id);
