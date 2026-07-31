-- H2-compatible stand-in migration, used only by DatabaseMigratorTest to
-- exercise a real Flyway run without Postgres-only syntax. The real
-- migrations under db/migration are verified against a live Postgres out of
-- band (see H2DatabaseBase's javadoc in kairos-api).
CREATE TABLE marker (
    id VARCHAR(36) PRIMARY KEY
);
