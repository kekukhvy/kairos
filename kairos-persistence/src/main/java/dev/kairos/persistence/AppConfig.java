package dev.kairos.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

/**
 * Configuration for the runnable Kairos components, resolved from three
 * sources in descending priority:
 *
 * <ol>
 *   <li>an environment variable — {@code db.url} reads {@code DB_URL};</li>
 *   <li>a JVM system property under the key itself ({@code -Ddb.url=...});</li>
 *   <li>{@code application.properties} on the classpath.</li>
 * </ol>
 *
 * <p>The environment layer is what makes a component containerizable: the same
 * image runs against any database by setting {@code DB_URL}, instead of baking
 * the address into the jar. Config therefore never has to be duplicated per
 * environment — the packaged defaults stay aimed at local development.
 */
public class AppConfig {

    private static final String PROPERTIES_FILE = "application.properties";
    private static final String KEY_SEPARATOR = ".";
    private static final String ENV_SEPARATOR = "_";

    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        try (InputStream inputStream = AppConfig.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {

            if (inputStream == null) {
                throw new IllegalStateException("Application properties file not found!");
            }

            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new AppConfig(properties);
    }

    /**
     * Returns the value for {@code key}, or throws if no source defines it.
     *
     * @throws IllegalStateException if the key is absent from every source
     */
    public String getProperty(String key) {
        return findProperty(key)
                .orElseThrow(() -> new IllegalStateException("Property " + key + " not found!"));
    }

    public int getIntProperty(String key, int defaultValue) {
        return findProperty(key)
                .map(value -> parseIntOrDefault(value, defaultValue))
                .orElse(defaultValue);
    }

    /**
     * Resolves a key across all sources, highest priority first. A blank value
     * counts as absent, so an empty environment variable falls through to the
     * packaged default rather than yielding an empty setting.
     */
    private Optional<String> findProperty(String key) {
        return firstNonBlank(System.getenv(toEnvVariableName(key)))
                .or(() -> firstNonBlank(System.getProperty(key)))
                .or(() -> firstNonBlank(properties.getProperty(key)));
    }

    private static Optional<String> firstNonBlank(String value) {
        return Optional.ofNullable(value).filter(candidate -> !candidate.isBlank());
    }

    /**
     * Maps a property key to its environment-variable form, upper-casing it and
     * replacing dots with underscores: {@code db.pool.size} → {@code DB_POOL_SIZE}.
     */
    private static String toEnvVariableName(String key) {
        return key.replace(KEY_SEPARATOR, ENV_SEPARATOR).toUpperCase();
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
