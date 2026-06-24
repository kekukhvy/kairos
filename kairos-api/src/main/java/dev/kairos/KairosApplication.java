package dev.kairos;

import dev.kairos.config.AppConfig;
import dev.kairos.config.DataSourceFactory;
import dev.kairos.config.DatabaseMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class KairosApplication {

    private static Logger logger = LoggerFactory.getLogger(KairosApplication.class);

    public static void main() {
        logger.info("tarting Kairos...");

        AppConfig appConfig = AppConfig.load();
        DataSource dataSource = DataSourceFactory.getDataSource(appConfig);
        DatabaseMigrator.migrate(dataSource, appConfig);

        logger.info("KairosApplication started");
    }
}
