package lms.report;
// TODO: add filters
// TODO: support export variants

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExporterTest {
    @TempDir

    @Test
    void export_writesCsvFile() throws Exception {
        Path file = tempDir.resolve("report.csv");
        exporter.export(file,
                Arrays.asList("Col1", "Col2"),
                Collections.singletonList(new String[] { "Hello,World", "He said \"Hi\"" }));

        String content = Files.readString(file);
        assertTrue(content.contains("Col1,Col2"));
        assertTrue(content.contains("\"Hello,World\""));
        assertTrue(content.contains("\"He said \"\"Hi\"\"\""));
    }
}