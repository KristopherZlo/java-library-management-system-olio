package lms.storage;

import java.nio.file.Path;
import lms.model.Book;
import lms.model.BookCopy;
import lms.model.CopyStatus;
import lms.model.Member;
import lms.model.MemberType;
import lms.storage.file.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void fileStorage_persistsEntities() {
        FileStorage storage = new FileStorage(tempDir.toString());
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        storage.books().save(book);
        storage.copies().save(new BookCopy("C1", book.getIsbn(), CopyStatus.AVAILABLE));
        storage.members().save(new Member("M1", "Alice", "a@example.com", MemberType.STUDENT));

        FileStorage reload = new FileStorage(tempDir.toString());
        assertTrue(reload.books().findById("9781234567890").isPresent());
        assertEquals(1, reload.copies().findAll().size());
        assertEquals(1, reload.members().findAll().size());
    }
}