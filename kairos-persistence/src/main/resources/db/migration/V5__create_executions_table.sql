-- executions: the current/upcoming work plan (hot, small table). The engine's
-- claim loop (SELECT ... FOR UPDATE SKIP LOCKED) operates on it. Size depends
-- only on near-horizon work, not on history volume.
CREATE TABLE executions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id         UUID         NOT NULL REFERENCES tasks (id),
    schedule_id     UUID         REFERENCES schedules (id),   -- null for ad-hoc runs
    scheduled_for   TIMESTAMPTZ  NOT NULL,        -- when this was supposed to fire
    next_attempt_at TIMESTAMPTZ  NOT NULL,        -- claim key for SKIP LOCKED, moves forward on retry
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',  -- PENDING | CLAIMED | RETRYING
    attempt         INT          NOT NULL DEFAULT 0,
    recycle         BOOLEAN      NOT NULL DEFAULT false,  -- true for FIXED: row reused, not deleted
    locked_by       VARCHAR(128),                 -- worker id
    locked_at       TIMESTAMPTZ,                  -- for detecting stuck claims
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT executions_status_check
        CHECK (status IN ('PENDING', 'CLAIMED', 'RETRYING'))
);

-- Claim loop: status IN (PENDING, RETRYING) AND next_attempt_at <= now().
CREATE INDEX idx_executions_claim
    ON executions (next_attempt_at)
    WHERE status IN ('PENDING', 'RETRYING');

-- Reaper for stuck CLAIMED rows uses locked_at.
CREATE INDEX idx_executions_locked_at
    ON executions (locked_at)
    WHERE status = 'CLAIMED';

CREATE INDEX idx_executions_task_id     ON executions (task_id);
CREATE INDEX idx_executions_schedule_id ON executions (schedule_id);
