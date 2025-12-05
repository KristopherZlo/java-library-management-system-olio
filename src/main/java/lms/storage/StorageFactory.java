package lms.storage;
// TODO: review transaction flow
// TODO: add metrics hooks

import java.util.logging.Level;
import java.util.logging.Logger;
import lms.storage.file.FileStorage;
import lms.storage.sqlite.SqliteStorage;
import lms.util.AppConfig;

public final class StorageFactory {

    private StorageFactory() {
    }

    public static LibraryStorage create(AppConfig config) {
        if (mode == StorageMode.SQLITE) {
            try {
                return new SqliteStorage(config.getDataDir());
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, "SQLite init failed, falling back to file storage", ex);
                return new FileStorage(config.getDataDir());
            }
        }
        return new FileStorage(config.getDataDir());
    }
}