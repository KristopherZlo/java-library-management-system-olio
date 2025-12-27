package lms.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import lms.storage.StorageMode;

public class AppConfig {
    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        Path path = Paths.get("config", "app.properties");
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            } catch (IOException ignored) {
                // Fall back to defaults if config can't be read.
            }
        }
        return new AppConfig(properties);
    }

    public StorageMode getStorageMode() {
        String raw = properties.getProperty("storage.mode", "SQLITE");
        try {
            return StorageMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return StorageMode.SQLITE;
        }
    }

    public String getDataDir() {
        return properties.getProperty("data.dir", "data");
    }

    public String getLogFile() {
        return properties.getProperty("log.file", "app.log");
    }

    public boolean isDemoEnabled() {
        String raw = properties.getProperty("demo.enabled", "true");
        return !raw.trim().equalsIgnoreCase("false");
    }
}