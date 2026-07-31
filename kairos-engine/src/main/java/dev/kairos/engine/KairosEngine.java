package dev.kairos.engine;

import dev.kairos.persistence.AppConfig;
import dev.kairos.persistence.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * Entry point for the {@code kairos-engine} runnable component.
 *
 * <p>Today the engine only owns and migrates the schema (see
 * {@code doc/specs/persistence-module.md}); the claim loop and delivery are
 * later milestones. After migrating it blocks on a latch released by a JVM
 * shutdown hook, so the process stays up like any other long-running
 * component and stops cleanly on SIGTERM/Ctrl-C — never via
 * {@code System.exit}.
 */
public final class KairosEngine {

    private static final Logger log = LoggerFactory.getLogger(KairosEngine.class);
    private static final String FLYWAY_LOCATIONS_KEY = "flyway.locations";
    private static final String HEALTH_FILE_KEY = "engine.health.file";

    public static void main(String[] args) {
        log.info("Starting kairos-engine...");

        AppConfig config = AppConfig.load();
        DataSource dataSource = DataSourceFactory.getDataSource(config);
        EngineHealth health = new EngineHealth(Path.of(config.getProperty(HEALTH_FILE_KEY)));

        new EngineBootstrap(dataSource, config.getProperty(FLYWAY_LOCATIONS_KEY), health).run();

        log.info("kairos-engine started, schema ready.");
        awaitShutdown();
    }

    private static void awaitShutdown() {
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Stopping kairos-engine...");
            shutdownLatch.countDown();
        }));

        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
