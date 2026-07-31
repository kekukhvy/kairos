-- Tighten the FIXED interval upper bound to one day (86400s). An interval
-- longer than a day is a calendar concern and belongs to CRON, not a plain
-- interval. Keeps the DB CHECK consistent with the Schedule domain invariant
-- (Schedule.MAX_INTERVAL_SECONDS).
ALTER TABLE schedules
    DROP CONSTRAINT schedules_type_fields_check;

ALTER TABLE schedules
    ADD CONSTRAINT schedules_type_fields_check CHECK (
        (type = 'ONCE'  AND run_at IS NOT NULL
                        AND cron_expression IS NULL AND interval_seconds IS NULL) OR
        (type = 'CRON'  AND cron_expression IS NOT NULL
                        AND run_at IS NULL AND interval_seconds IS NULL) OR
        (type = 'FIXED' AND interval_seconds IS NOT NULL
                        AND interval_seconds > 0 AND interval_seconds <= 86400
                        AND run_at IS NULL AND cron_expression IS NULL)
    );
