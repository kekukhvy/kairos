-- H2-compatible stand-in migration used only by EngineBootstrapTest, to
-- exercise the real Flyway run without depending on Postgres-only syntax
-- (JSONB, TIMESTAMPTZ) that the real V1..V8 migrations use. The real
-- migrations are verified against a live Postgres out of band; this test
-- verifies EngineBootstrap's own ordering contract (migrate, then mark ready).
CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY
);
