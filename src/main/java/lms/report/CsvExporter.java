package lms.report;
// TODO: add filters
// TODO: support export variants

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lms.exception.StorageException;

public class CsvExporter {
    public void export(Path path, List<String> headers, List<String[]> rows) {
        try {
            if (path.getParent() != null) {
            }
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                if (headers != null && !headers.isEmpty()) {
                    writer.newLine();
                }
                for (String[] row : rows) {
                    writer.write(join(row));
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            throw new StorageException("Failed to export CSV to " + path, ex);
        }
    }

    private String join(String[] fields) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(escape(fields[i]));
        }
        return builder.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}