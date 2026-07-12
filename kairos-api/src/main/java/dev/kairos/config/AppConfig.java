package dev.kairos.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        try (InputStream inputStream = AppConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (inputStream == null) {
                throw new IllegalStateException("Application properties file not found!");
            }

            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new AppConfig(properties);
    }

    public String getProperty(String key) {
        if (properties.containsKey(key)) {
            return properties.getProperty(key);
        } else {
            throw new IllegalStateException("Property " + key + " not found!");
        }
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
