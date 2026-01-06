package lms.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lms.model.Book;
import lms.model.BookCopy;
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

        List<BookSeed> books = buildBookSeeds();
        int index = 0;
        for (BookSeed seed : books) {
            Book book = new Book(seed.isbn, seed.title, seed.author, seed.year, seed.genre);
            book.setBookId(String.format("BOOK-%03d", index + 1));
            service.addBook(book, seed.copies);
            index++;
        }

        List<Member> members = Arrays.asList(
                new Member("MEM-001", "Aino Laine", "aino.laine@example.com", MemberType.STUDENT),
                new Member("MEM-002", "Mikko Rinne", "mikko.rinne@example.com", MemberType.STUDENT),
                new Member("MEM-003", "Laura Kaski", "laura.kaski@example.com", MemberType.STUDENT),
                new Member("MEM-004", "Olli Mattila", "olli.mattila@example.com", MemberType.ADULT),
                new Member("MEM-005", "Sari Lehto", "sari.lehto@example.com", MemberType.ADULT),
                new Member("MEM-006", "Antti Salmi", "antti.salmi@example.com", MemberType.ADULT),
                new Member("MEM-007", "Eeva Karhu", "eeva.karhu@example.com", MemberType.STUDENT),
                new Member("MEM-008", "Jari Holm", "jari.holm@example.com", MemberType.STUDENT),
                new Member("MEM-009", "Noora Helmi", "noora.helmi@example.com", MemberType.STUDENT),
                new Member("MEM-010", "Ville Saari", "ville.saari@example.com", MemberType.STUDENT),
                new Member("MEM-011", "Marja Kivi", "marja.kivi@example.com", MemberType.ADULT),
                new Member("MEM-012", "Pekka Vuori", "pekka.vuori@example.com", MemberType.ADULT),
                new Member("MEM-013", "Tuuli Nyman", "tuuli.nyman@example.com", MemberType.ADULT),
                new Member("MEM-014", "Kari Aaltonen", "kari.aaltonen@example.com", MemberType.ADULT),
                new Member("MEM-015", "Hanna Arto", "hanna.arto@example.com", MemberType.STUDENT),
                new Member("MEM-016", "Elias Niemi", "elias.niemi@example.com", MemberType.STUDENT)
        );
        for (Member member : members) {
            service.addMember(member);
        }

        String[] memberIds = members.stream().map(Member::getMemberId).toArray(String[]::new);
        LocalDate today = LocalDate.now();

        String[] starterLoans = {
                books.get(0).isbn,
                books.get(1).isbn,
                books.get(2).isbn,
                books.get(3).isbn,
                books.get(4).isbn,
                books.get(5).isbn,
                books.get(6).isbn,
                books.get(7).isbn,
                books.get(20).isbn,
                books.get(21).isbn,
                books.get(22).isbn,
                books.get(23).isbn
        };
        for (int i = 0; i < starterLoans.length; i++) {
            service.loanByIsbn(starterLoans[i], memberIds[i]);
        }

        service.loanByIsbn(books.get(24).isbn, memberIds[0]);
        service.loanByIsbn(books.get(25).isbn, memberIds[1]);
        service.loanByIsbn(books.get(26).isbn, memberIds[2]);

        service.loanByIsbn(books.get(30).isbn, memberIds[3], today.minusDays(45), today.minusDays(15));
        service.loanByIsbn(books.get(31).isbn, memberIds[4], today.minusDays(35), today.minusDays(14));
        service.loanByIsbn(books.get(32).isbn, memberIds[6], today.minusDays(60), today.minusDays(30));

        BookSeed readyReservationBook = books.get(8);
        Loan readyLoan = service.loanByIsbn(readyReservationBook.isbn, memberIds[12]);
        service.reserveByIsbn(readyReservationBook.isbn, memberIds[13]);
        service.reserveByIsbn(readyReservationBook.isbn, memberIds[14]);
        service.returnByCopyId(readyLoan.getCopyId());

        BookSeed queuedReservationBook = books.get(9);
        service.loanByIsbn(queuedReservationBook.isbn, memberIds[15]);
        service.reserveByIsbn(queuedReservationBook.isbn, memberIds[0]);
        service.reserveByIsbn(queuedReservationBook.isbn, memberIds[1]);

        Loan historyLoan = service.loanByIsbn(books.get(28).isbn, memberIds[7],
                today.minusDays(18), today.minusDays(3));
        service.returnByCopyId(historyLoan.getCopyId());
        Loan historyLoanTwo = service.loanByIsbn(books.get(29).isbn, memberIds[8],
                today.minusDays(26), today.minusDays(6));
        service.returnByCopyId(historyLoanTwo.getCopyId());
        Loan historyLoanThree = service.loanByIsbn(books.get(34).isbn, memberIds[9],
                today.minusDays(14), today.minusDays(2));
        service.returnByCopyId(historyLoanThree.getCopyId());

        List<BookCopy> copies = service.getCopiesByIsbn(books.get(27).isbn);
        if (!copies.isEmpty()) {
            service.markCopyLost(copies.get(0).getCopyId());
        }
    }

    private static List<BookSeed> buildBookSeeds() {
        return Arrays.asList(
                new BookSeed("9780000000001", "Northern Skies", "A. Koskinen", 1999, "Fantasy", 2),
                new BookSeed("9780000000002", "Code Patterns", "J. Niemi", 2008, "Technology", 2),
                new BookSeed("9780000000003", "Winter Roads", "L. Laine", 2003, "Drama", 2),
                new BookSeed("9780000000004", "Deep Sea", "M. Virtanen", 2012, "Adventure", 2),
                new BookSeed("9780000000005", "Quiet Forest", "E. Saarinen", 2001, "Nature", 2),
                new BookSeed("9780000000006", "Stone Harbor", "T. Kallio", 2015, "Mystery", 2),
                new BookSeed("9780000000007", "Morning Light", "S. Heikkinen", 2010, "Romance", 2),
                new BookSeed("9780000000008", "The Last Signal", "R. Lehto", 2018, "Sci-Fi", 2),
                new BookSeed("9780000000009", "Hidden Trails", "K. Hiltunen", 2006, "Adventure", 1),
                new BookSeed("9780000000010", "City Lines", "P. Maki", 2014, "Drama", 1),
                new BookSeed("9780000000011", "Iron Gate", "V. Salminen", 1998, "History", 1),
                new BookSeed("9780000000012", "White Lake", "H. Nieminen", 2005, "Nature", 1),
                new BookSeed("9780000000013", "Silent Code", "D. Aaltonen", 2020, "Technology", 1),
                new BookSeed("9780000000014", "Arctic Dream", "N. Korhonen", 2011, "Fantasy", 1),
                new BookSeed("9780000000015", "Broken Compass", "I. Lehtinen", 2009, "Thriller", 1),
                new BookSeed("9780000000016", "Golden Path", "O. Ranta", 2007, "Adventure", 1),
                new BookSeed("9780000000017", "Far Horizon", "C. Laakso", 2016, "Sci-Fi", 1),
                new BookSeed("9780000000018", "Old Harbor", "B. Leppanen", 1995, "History", 1),
                new BookSeed("9780000000019", "Paper Wings", "F. Salmi", 2013, "Romance", 1),
                new BookSeed("9780000000020", "Blue Minute", "J. Kuusela", 2021, "Drama", 1),
                new BookSeed("9780000000021", "Signal Noise", "A. Salonen", 2017, "Sci-Fi", 3),
                new BookSeed("9780000000022", "Glass City", "M. Aalto", 2019, "Mystery", 3),
                new BookSeed("9780000000023", "Tundra Echo", "E. Kivinen", 2002, "Nature", 3),
                new BookSeed("9780000000024", "Midnight Route", "L. Rinne", 2004, "Thriller", 3),
                new BookSeed("9780000000025", "Open Waters", "S. Niemi", 2000, "Adventure", 3),
                new BookSeed("9780000000026", "Hidden Courtyard", "I. Niemi", 2006, "Mystery", 2),
                new BookSeed("9780000000027", "River Atlas", "R. Hattunen", 2011, "Science", 2),
                new BookSeed("9780000000028", "Paper Signals", "N. Vartiainen", 2015, "Technology", 2),
                new BookSeed("9780000000029", "Lantern Hill", "O. Toivonen", 2001, "Children", 4),
                new BookSeed("9780000000030", "Midnight Algebra", "P. Ranta", 2018, "Education", 2),
                new BookSeed("9780000000031", "The Archivist", "S. Nieminen", 2009, "Thriller", 1),
                new BookSeed("9780000000032", "Glass Orchard", "E. Hyttinen", 2004, "Drama", 1),
                new BookSeed("9780000000033", "Quiet Machines", "L. Aaltonen", 2022, "Technology", 2),
                new BookSeed("9780000000034", "Snowbound Letters", "K. Koski", 1997, "Romance", 1),
                new BookSeed("9780000000035", "Tideclock", "M. Hiltunen", 2013, "Adventure", 2),
                new BookSeed("9780000000036", "Amber Maps", "T. Vuori", 1993, "History", 1),
                new BookSeed("9780000000037", "Second Sunrise", "J. Halme", 2020, "Sci-Fi", 2),
                new BookSeed("9780000000038", "Practice of Care", "A. Leppala", 2016, "Health", 1),
                new BookSeed("9780000000039", "City of Graphs", "R. Manner", 2019, "Business", 2),
                new BookSeed("9780000000040", "Wolves of the North", "V. Karjalainen", 2005, "Fantasy", 2)
        );
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