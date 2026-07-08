-- tasks: what to deliver, where, and with which retry policy.
-- A stable entity that changes rarely. Supports soft delete via deleted_at.
CREATE TABLE tasks (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    service        VARCHAR(128) NOT NULL,           -- owning service
    name           VARCHAR(255) NOT NULL,           -- e.g. 'expire-booking'
    description    TEXT,                            -- nullable
    active         BOOLEAN      NOT NULL DEFAULT true,  -- overall kill-switch for the whole task
    destination_id VARCHAR(128) NOT NULL REFERENCES destinations (id),
    event_name     VARCHAR(255) NOT NULL,           -- machine event id for consumer routing, e.g. 'booking.expire.v1'
    payload        JSONB,                           -- default payload
    timeout_ms     INT          NOT NULL,           -- delivery timeout
    supports_retry BOOLEAN      NOT NULL DEFAULT false,  -- if false, retry_policies is not used
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,                     -- soft delete: not null => task is deleted

    -- Denormalized run summary, maintained by the engine for fast task listing.
    -- Source of truth stays in executions / execution_history; these are a cache.
    last_status    VARCHAR(16),                     -- last run outcome: SUCCESS | FAILED | DEAD_LETTER; null until first run
    last_run_at    TIMESTAMPTZ,                     -- when the task last ran; null until first run
    next_run_at    TIMESTAMPTZ,                     -- next planned run; null if nothing scheduled

    CONSTRAINT tasks_timeout_ms_check CHECK (timeout_ms > 0),
    CONSTRAINT tasks_last_status_check
        CHECK (last_status IS NULL OR last_status IN ('SUCCESS', 'FAILED', 'DEAD_LETTER'))
);

-- Normal reads (GET/LIST) filter WHERE deleted_at IS NULL; index the live rows.
CREATE INDEX idx_tasks_active_not_deleted
    ON tasks (created_at)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_tasks_destination_id ON tasks (destination_id);
