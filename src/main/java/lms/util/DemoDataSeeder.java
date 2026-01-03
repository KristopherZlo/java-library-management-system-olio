package lms.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lms.model.Book;
import lms.model.Loan;
import lms.model.Member;
import lms.model.MemberType;
import lms.service.LibraryService;

public final class DemoDataSeeder {
    private DemoDataSeeder() {
    }

    public static void seedIfEmpty(LibraryService service) {
        if (!service.listBooks().isEmpty()) {
            return;
        }

        // TODO: expand demo catalog
        // TODO: add overdue and reservation samples
        List<BookSeed> books = Arrays.asList(
                new BookSeed("9780000000001", "Northern Skies", "A. Koskinen", 1999, "Fantasy", 2),
                new BookSeed("9780000000002", "Code Patterns", "J. Niemi", 2008, "Technology", 1),
                new BookSeed("9780000000003", "Winter Roads", "L. Laine", 2003, "Drama", 1)
        );
        int index = 0;
        for (BookSeed seed : books) {
            Book book = new Book(seed.isbn, seed.title, seed.author, seed.year, seed.genre);
            book.setBookId(String.format("BOOK-%03d", index + 1));
            service.addBook(book, seed.copies);
            index++;
        }

        List<Member> members = Arrays.asList(
                new Member("MEM-001", "Aino Laine", "aino.laine@example.com", MemberType.STUDENT),
                new Member("MEM-002", "Mikko Rinne", "mikko.rinne@example.com", MemberType.ADULT)
        );
        for (Member member : members) {
            service.addMember(member);
        }

        Loan loan = service.loanByIsbn(books.get(0).isbn, members.get(0).getMemberId(),
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        service.returnByCopyId(loan.getCopyId());
    }

    private static class BookSeed {
        private final String isbn;
        private final String title;
        private final String author;
        private final int year;
        private final String genre;
        private final int copies;

        private BookSeed(String isbn, String title, String author, int year, String genre, int copies) {
            this.isbn = isbn;
            this.title = title;
            this.author = author;
            this.year = year;
            this.genre = genre;
            this.copies = copies;
        }
    }
}