package lms.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import lms.exception.StorageException;
import lms.storage.StorageMode;

public final class AppConfigWriter {
    private AppConfigWriter() {
    }

    public static void updateStorageMode(StorageMode mode) {
        Path path = Paths.get("config", "app.properties");
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try {
                properties.load(Files.newInputStream(path));
            } catch (IOException ignored) {
                // Use defaults if existing config cannot be loaded.
            }
        }
        properties.setProperty("storage.mode", mode.name());
        properties.putIfAbsent("data.dir", "data");
        properties.putIfAbsent("log.file", "app.log");
        properties.putIfAbsent("demo.enabled", "true");
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, "Library Management System");
            }
        } catch (IOException ex) {
            throw new StorageException("Failed to update config", ex);
        }
    }
}