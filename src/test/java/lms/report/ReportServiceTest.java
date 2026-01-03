package lms.report;
// TODO: add filters
// TODO: support export variants

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import lms.model.Book;
import lms.model.BookCopy;
import lms.model.CopyStatus;
import lms.model.Loan;
import lms.model.Member;
import lms.model.MemberType;
import lms.policy.LoanPolicyResolver;
import lms.policy.PerDayFinePolicy;
import lms.service.LibraryService;
import lms.storage.file.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReportServiceTest {
    @TempDir

    private LibraryService service;
    private ReportService reports;

    @BeforeEach
    void setUp() {
        storage = new FileStorage(tempDir.toString());
        service = new LibraryService(storage, new LoanPolicyResolver(),
                new PerDayFinePolicy(50), () -> LocalDate.of(2024, 1, 10));
        reports = new ReportService(storage, service, () -> LocalDate.of(2024, 1, 10));
    }

    @Test
    void buildOverdueReport_includesFineAndDays() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        storage.books().save(book);
        storage.copies().save(new BookCopy("C1", book.getIsbn(), CopyStatus.LOANED));
        storage.members().save(new Member("M1", "Alice", "a@example.com", MemberType.STUDENT));
        storage.loans().save(new Loan("L1", "C1", "M1",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5)));

        List<OverdueReportItem> items = reports.buildOverdueReport(LocalDate.of(2024, 1, 10));
        assertEquals(1, items.size());
        OverdueReportItem item = items.get(0);
        assertEquals(5, item.getDaysOverdue());
        assertEquals(250, item.getFineCents());
    }

    @Test
    void buildMemberReport_returnsActiveLoans() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        storage.books().save(book);
        storage.copies().save(new BookCopy("C1", book.getIsbn(), CopyStatus.LOANED));
        storage.members().save(new Member("M1", "Alice", "a@example.com", MemberType.STUDENT));
        storage.loans().save(new Loan("L1", "C1", "M1",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5)));

        List<MemberLoanReportItem> items = reports.buildMemberReport("M1");
        assertEquals(1, items.size());
        assertEquals("L1", items.get(0).getLoanId());
    }

    @Test
    void buildPopularBooksReport_ordersByLoans() {
        Book book1 = new Book("9781234567890", "First", "Author", 2023, "Fiction");
        book1.setTotalLoans(2);
        Book book2 = new Book("9781234567891", "Second", "Author", 2023, "Fiction");
        book2.setTotalLoans(5);
        storage.books().save(book1);
        storage.books().save(book2);

        List<PopularBookItem> items = reports.buildPopularBooksReport(10);
        assertFalse(items.isEmpty());
        assertEquals("9781234567891", items.get(0).getIsbn());
    }
}