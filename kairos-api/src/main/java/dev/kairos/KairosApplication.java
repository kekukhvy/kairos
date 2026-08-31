package dev.kairos;

import dev.kairos.persistence.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KairosApplication {

    private static final Logger log = LoggerFactory.getLogger(KairosApplication.class);

    public static void main(String[] args) {
        log.info("Starting Kairos...");

        AppConfig config = AppConfig.load();
        ApplicationContext.build(config).start();

        log.info("Kairos started");
    }
}