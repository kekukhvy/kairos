package dev.kairos.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceFactory {

    public static DataSource getDataSource(AppConfig config) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(config.getProperty("db.url"));
        cfg.setUsername(config.getProperty("db.username"));
        cfg.setPassword(config.getProperty("db.password"));
        cfg.setMaximumPoolSize(config.getIntProperty("db.pool.size", 10));
        cfg.setPoolName("kairos-pool");
        return new HikariDataSource(cfg);
    }
}
