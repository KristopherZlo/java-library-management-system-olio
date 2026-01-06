package lms.service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import lms.exception.RuleViolationException;
import lms.model.Book;
import lms.model.BookCopy;
import lms.model.CopyStatus;
import lms.model.Loan;
import lms.model.Member;
import lms.model.MemberType;
import lms.model.Reservation;
import lms.model.ReservationStatus;
import lms.policy.LoanPolicyResolver;
import lms.policy.PerDayFinePolicy;
import lms.storage.file.FileStorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryServiceTest {
    @TempDir
    Path tempDir;

    private LibraryService service;
    private FixedDateProvider dateProvider;

    @BeforeEach
    void setUp() {
        FileStorage storage = new FileStorage(tempDir.toString());
        dateProvider = new FixedDateProvider(LocalDate.of(2024, 1, 1));
        service = new LibraryService(storage, new LoanPolicyResolver(), new PerDayFinePolicy(50), dateProvider);
    }

    @Test
    void addBook_createsCopies() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 2);
        assertNotNull(book.getBookId());
        assertEquals(2, service.getCopiesByIsbn("9781234567890").size());
    }

    @Test
    void loanByIsbn_usesAvailableCopy() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 1);
        Member member = new Member("MEM-1", "Alice", "a@example.com", MemberType.STUDENT);
        service.addMember(member);

        Loan loan = service.loanByIsbn("9781234567890", "MEM-1");
        assertNotNull(loan.getCopyId());
        assertEquals(LocalDate.of(2024, 1, 1), loan.getLoanDate());
    }

    @Test
    void loanByBookId_works() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        book.setBookId("BOOK-XYZ");
        service.addBook(book, 1);
        Member member = new Member("MEM-1", "Alice", "a@example.com", MemberType.STUDENT);
        service.addMember(member);

        Loan loan = service.loanByIsbn("BOOK-XYZ", "MEM-1");
        assertNotNull(loan.getCopyId());
        assertEquals(1, service.getCopiesByIsbn("BOOK-XYZ").size());
    }

    @Test
    void addCopyByBookId_works() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        book.setBookId("BOOK-ABC");
        service.addBook(book, 0);

        BookCopy copy = service.addCopy("BOOK-ABC");
        assertEquals(book.getIsbn(), copy.getIsbn());
    }

    @Test
    void searchBooks_fuzzyMatchesTypos() {
        Book book = new Book("9781234567890", "Northern Skies", "A. Koskinen", 1999, "Fantasy");
        service.addBook(book, 0);

        boolean found = service.searchBooks("Nothern Skies").stream()
                .anyMatch(result -> result.getIsbn().equals(book.getIsbn()));
        assertTrue(found);
    }

    @Test
    void searchBooks_ordersBySimilarity() {
        Book exact = new Book("9781234567890", "Northern Skies", "A. Koskinen", 1999, "Fantasy");
        Book near = new Book("9781234567891", "Nothern Skies", "B. Laine", 2001, "Fantasy");
        service.addBook(exact, 0);
        service.addBook(near, 0);

        List<Book> results = service.searchBooks("Northern Skies");
        assertTrue(results.size() >= 2);
        assertEquals(exact.getIsbn(), results.get(0).getIsbn());
    }

    @Test
    void searchBooks_shortQueryFindsNearMatches() {
        Book book = new Book("9781234567890", "Open Waters", "S. Niemi", 2000, "Adventure");
        service.addBook(book, 0);

        List<Book> results = service.searchBooks("olen");
        assertFalse(results.isEmpty());
        assertEquals(book.getIsbn(), results.get(0).getIsbn());
    }

    @Test
    void loanByIsbn_respectsLoanLimit() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 4);
        Member member = new Member("MEM-1", "Bob", "b@example.com", MemberType.ADULT);
        service.addMember(member);

        service.loanByIsbn("9781234567890", "MEM-1");
        service.addCopy("9781234567890");
        service.loanByIsbn("9781234567890", "MEM-1");
        service.addCopy("9781234567890");
        service.loanByIsbn("9781234567890", "MEM-1");

        assertThrows(RuleViolationException.class,
                () -> service.loanByIsbn("9781234567890", "MEM-1"));
    }

    @Test
    void reserveByIsbn_queuesWhenUnavailable() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 1);
        Member member = new Member("MEM-1", "Carol", "c@example.com", MemberType.STUDENT);
        service.addMember(member);
        service.loanByIsbn("9781234567890", "MEM-1");

        Member member2 = new Member("MEM-2", "Dan", "d@example.com", MemberType.STUDENT);
        service.addMember(member2);
        Reservation reservation = service.reserveByIsbn("9781234567890", "MEM-2");
        assertEquals(ReservationStatus.QUEUED, reservation.getStatus());
    }

    @Test
    void reserveByIsbn_failsWhenAvailable() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 1);
        Member member = new Member("MEM-1", "Eve", "e@example.com", MemberType.STUDENT);
        service.addMember(member);

        assertThrows(RuleViolationException.class,
                () -> service.reserveByIsbn("9781234567890", "MEM-1"));
    }

    @Test
    void returnByCopyId_promotesReservation() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 1);
        Member member = new Member("MEM-1", "Fred", "f@example.com", MemberType.STUDENT);
        service.addMember(member);
        Loan loan = service.loanByIsbn("9781234567890", "MEM-1");

        Member member2 = new Member("MEM-2", "Gina", "g@example.com", MemberType.STUDENT);
        service.addMember(member2);
        service.reserveByIsbn("9781234567890", "MEM-2");

        ReturnResult result = service.returnByCopyId(loan.getCopyId());
        assertNotNull(result.getReadyReservation());
        assertEquals(ReservationStatus.READY, result.getReadyReservation().getStatus());
        assertEquals(CopyStatus.RESERVED, result.getReservedCopy().getStatus());
    }

    @Test
    void removeBook_failsWithActiveLoan() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 1);
        Member member = new Member("MEM-1", "Hugo", "h@example.com", MemberType.STUDENT);
        service.addMember(member);
        service.loanByIsbn("9781234567890", "MEM-1");

        assertThrows(RuleViolationException.class,
                () -> service.removeBook("9781234567890"));
    }

    @Test
    void calculateFine_usesPolicy() {
        Loan loan = new Loan("L1", "C1", "M1",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 5));
        long fine = service.calculateFineCents(loan, LocalDate.of(2024, 1, 10));
        assertEquals(250, fine);
    }

    @Test
    void updateLoanDate_shiftsDueDate() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 1);
        Member member = new Member("MEM-1", "Alice", "a@example.com", MemberType.STUDENT);
        service.addMember(member);
        Loan loan = service.loanByIsbn("9781234567890", "MEM-1");

        long days = ChronoUnit.DAYS.between(loan.getLoanDate(), loan.getDueDate());
        LocalDate newLoanDate = LocalDate.of(2024, 1, 10);
        Loan updated = service.updateLoanDate(loan.getLoanId(), newLoanDate);

        assertEquals(newLoanDate, updated.getLoanDate());
        assertEquals(newLoanDate.plusDays(days), updated.getDueDate());
    }

    @Test
    void updateLoanDueDate_changesDueDate() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 1);
        Member member = new Member("MEM-1", "Alice", "a@example.com", MemberType.STUDENT);
        service.addMember(member);
        Loan loan = service.loanByIsbn("9781234567890", "MEM-1");

        LocalDate newDueDate = loan.getLoanDate().plusDays(5);
        Loan updated = service.updateLoanDueDate(loan.getLoanId(), newDueDate);

        assertEquals(newDueDate, updated.getDueDate());
    }

    @Test
    void loanByIsbn_allowsCustomDueDate() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 1);
        Member member = new Member("MEM-1", "Alice", "a@example.com", MemberType.STUDENT);
        service.addMember(member);

        LocalDate dueDate = LocalDate.of(2024, 2, 1);
        Loan loan = service.loanByIsbn("9781234567890", "MEM-1", null, dueDate);

        assertEquals(dueDate, loan.getDueDate());
    }

    @Test
    void searchMembers_fuzzyMatchesNameAndId() {
        Member member = new Member("MEM-123", "Northern Finch", "n@example.com", MemberType.STUDENT);
        service.addMember(member);

        boolean foundByName = service.searchMembers("Northrn Fin").stream()
                .anyMatch(result -> result.getMemberId().equals(member.getMemberId()));
        boolean foundById = service.searchMembers("MEM-12").stream()
                .anyMatch(result -> result.getMemberId().equals(member.getMemberId()));

        assertTrue(foundByName);
        assertTrue(foundById);
    }

    @Test
    void removeReservation_releasesReservedCopyWhenReady() {
        Book book = new Book("9781234567890", "Title", "Author", 2023, "Fiction");
        service.addBook(book, 1);
        Member member1 = new Member("MEM-1", "Alice", "a@example.com", MemberType.STUDENT);
        Member member2 = new Member("MEM-2", "Bob", "b@example.com", MemberType.STUDENT);
        service.addMember(member1);
        service.addMember(member2);

        Loan loan = service.loanByIsbn("9781234567890", "MEM-1");
        Reservation reservation = service.reserveByIsbn("9781234567890", "MEM-2");
        service.returnByCopyId(loan.getCopyId());

        boolean hasReserved = service.getCopiesByIsbn("9781234567890").stream()
                .anyMatch(copy -> copy.getStatus() == CopyStatus.RESERVED);
        assertTrue(hasReserved);

        service.removeReservation(reservation.getReservationId());

        boolean hasReservedAfter = service.getCopiesByIsbn("9781234567890").stream()
                .anyMatch(copy -> copy.getStatus() == CopyStatus.RESERVED);
        assertFalse(hasReservedAfter);
        assertTrue(service.listReservations().isEmpty());
    }

    private static class FixedDateProvider implements lms.util.DateProvider {
        private final LocalDate today;

        private FixedDateProvider(LocalDate today) {
            this.today = today;
        }

        @Override
        public LocalDate today() {
            return today;
        }
    }
}