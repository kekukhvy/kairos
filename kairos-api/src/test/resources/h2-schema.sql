-- H2-compatible schema for repository integration tests (PostgreSQL mode).
-- Mirrors the Postgres Flyway migrations (V1/V2) for the only two tables the
-- M1 repositories touch: destinations and tasks. The production schema stays
-- in db/migration; this file exists because a few Postgres-isms in those
-- migrations (gen_random_uuid() defaults, partial indexes) don't translate to
-- H2 — and the repositories never rely on them.

CREATE TABLE destinations (
    id         VARCHAR(128) PRIMARY KEY,
    type       VARCHAR(32)  NOT NULL,
    -- TEXT, not JSONB: H2's JSON/JSONB types wrap a setString value as a quoted
    -- JSON string literal, which breaks the raw-String round-trip TaskMapper
    -- expects. On real Postgres these columns are JSONB; the domain treats the
    -- value as an opaque String either way.
    config     TEXT         NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT destinations_type_check
        CHECK (type IN ('KAFKA', 'SQS', 'WEBHOOK', 'RABBITMQ'))
);

CREATE TABLE tasks (
    id             UUID         PRIMARY KEY,
    service        VARCHAR(128) NOT NULL,
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    active         BOOLEAN      NOT NULL DEFAULT true,
    destination_id VARCHAR(128) NOT NULL REFERENCES destinations (id),
    event_name     VARCHAR(255) NOT NULL,
    payload        TEXT,        -- TEXT not JSONB on H2; see destinations.config note
    timeout_ms     INT          NOT NULL,
    supports_retry BOOLEAN      NOT NULL DEFAULT false,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMP WITH TIME ZONE,

    last_status    VARCHAR(16),
    last_run_at    TIMESTAMP WITH TIME ZONE,
    next_run_at    TIMESTAMP WITH TIME ZONE,

    CONSTRAINT tasks_timeout_ms_check CHECK (timeout_ms > 0),
    CONSTRAINT tasks_last_status_check
        CHECK (last_status IS NULL OR last_status IN ('SUCCESS', 'FAILED', 'DEAD_LETTER'))
);

CREATE INDEX idx_tasks_created_at ON tasks (created_at);
CREATE INDEX idx_tasks_destination_id ON tasks (destination_id);

-- schedules: mirrors V4 + V7 migrations (H2-compatible; no gen_random_uuid() default,
-- no partial index on WHERE, no TIMESTAMPTZ alias — H2 uses TIMESTAMP WITH TIME ZONE).
CREATE TABLE schedules (
    id               UUID         PRIMARY KEY,
    task_id          UUID         NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    label            VARCHAR(128),
    type             VARCHAR(16)  NOT NULL,
    run_at           TIMESTAMP WITH TIME ZONE,
    cron_expression  VARCHAR(128),
    interval_seconds INT,
    timezone         VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    active           BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT schedules_type_check
        CHECK (type IN ('ONCE', 'CRON', 'FIXED')),

    -- V4 + V7 combined: FIXED upper-bound tightened to 86400.
    CONSTRAINT schedules_type_fields_check CHECK (
        (type = 'ONCE'  AND run_at IS NOT NULL
                        AND cron_expression IS NULL AND interval_seconds IS NULL) OR
        (type = 'CRON'  AND cron_expression IS NOT NULL
                        AND run_at IS NULL AND interval_seconds IS NULL) OR
        (type = 'FIXED' AND interval_seconds IS NOT NULL
                        AND interval_seconds > 0 AND interval_seconds <= 86400
                        AND run_at IS NULL AND cron_expression IS NULL)
    )
);

CREATE INDEX idx_schedules_task_id ON schedules (task_id);
