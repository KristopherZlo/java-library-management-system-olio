package lms.util;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {
    }

    public static String newId(String prefix) {
        String raw = UUID.randomUUID().toString().replace("-", "");
        return prefix + "-" + raw.substring(0, 8).toUpperCase();
    }
}