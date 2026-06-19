-- schedules: the "when" rule. One task can have many schedules (e.g. weekday
-- and weekend rules). Pausing works per schedule, independent of tasks.active.
CREATE TABLE schedules (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id          UUID         NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    label            VARCHAR(128),                  -- e.g. 'weekday-morning', 'weekend'
    type             VARCHAR(16)  NOT NULL,         -- ONCE | CRON | FIXED
    run_at           TIMESTAMPTZ,                   -- required for ONCE
    cron_expression  VARCHAR(128),                  -- required for CRON
    interval_seconds INT,                           -- required for FIXED
    timezone         VARCHAR(64)  NOT NULL DEFAULT 'UTC',  -- relevant for CRON
    active           BOOLEAN      NOT NULL DEFAULT true,   -- pauses this single rule
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT schedules_type_check
        CHECK (type IN ('ONCE', 'CRON', 'FIXED')),

    -- Each type requires exactly its own "when" field to be present.
    CONSTRAINT schedules_type_fields_check CHECK (
        (type = 'ONCE'  AND run_at IS NOT NULL
                        AND cron_expression IS NULL AND interval_seconds IS NULL) OR
        (type = 'CRON'  AND cron_expression IS NOT NULL
                        AND run_at IS NULL AND interval_seconds IS NULL) OR
        (type = 'FIXED' AND interval_seconds IS NOT NULL AND interval_seconds > 0
                        AND run_at IS NULL AND cron_expression IS NULL)
    )
);

CREATE INDEX idx_schedules_task_id ON schedules (task_id);

-- The planner walks active schedules to materialize executions.
CREATE INDEX idx_schedules_active ON schedules (active) WHERE active = true;
