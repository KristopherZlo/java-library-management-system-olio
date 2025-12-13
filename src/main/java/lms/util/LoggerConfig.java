package lms.util;
// TODO: review edge cases
// TODO: add test notes

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class LoggerConfig {
    private LoggerConfig() {
    }

    public static void configure(AppConfig config) {
        for (Handler handler : root.getHandlers()) {
        }
        root.setLevel(Level.INFO);

        Path logPath = Paths.get(config.getLogFile());
        Path parent = logPath.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException ignored) {
                // Log file dir may already exist or be unreachable.
            }
        }

        try {
            FileHandler fileHandler = new FileHandler(logPath.toString(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            root.addHandler(fileHandler);
        } catch (IOException ex) {
            root.log(Level.WARNING, "Failed to configure file logging", ex);
        }
    }
}