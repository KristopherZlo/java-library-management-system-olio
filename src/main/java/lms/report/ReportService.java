package lms.report;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lms.model.Book;
import lms.model.BookCopy;
import lms.model.Loan;
import lms.model.Member;
import lms.service.LibraryService;
import lms.storage.LibraryStorage;
import lms.storage.Repository;
import lms.util.DateProvider;

public class ReportService {
    private final Repository<Book, String> books;
    private final Repository<BookCopy, String> copies;
    private final Repository<Member, String> members;
    private final Repository<Loan, String> loans;
    private final LibraryService libraryService;
    private final DateProvider dateProvider;

    public ReportService(LibraryStorage storage, LibraryService libraryService, DateProvider dateProvider) {
        this.books = storage.books();
        this.copies = storage.copies();
        this.members = storage.members();
        this.loans = storage.loans();
        this.libraryService = libraryService;
        this.dateProvider = dateProvider;
    }

    public List<OverdueReportItem> buildOverdueReport(LocalDate today) {
        LocalDate date = today == null ? dateProvider.today() : today;
        return loans.findAll().stream()
                .filter(loan -> loan.isOverdue(date))
                .map(loan -> {
                    BookCopy copy = copies.findById(loan.getCopyId()).orElse(null);
                    Book book = copy == null ? null : books.findById(copy.getIsbn()).orElse(null);
                    Member member = members.findById(loan.getMemberId()).orElse(null);
                    long days = ChronoUnit.DAYS.between(loan.getDueDate(), date);
                    long fine = libraryService.calculateFineCents(loan, date);
                    return new OverdueReportItem(
                            book == null ? "-" : book.getIsbn(),
                            book == null ? "-" : book.getTitle(),
                            member == null ? loan.getMemberId() : member.getMemberId(),
                            member == null ? "-" : member.getName(),
                            loan.getDueDate(),
                            days,
                            fine
                    );
                })
                .collect(Collectors.toList());
    }

    public List<MemberLoanReportItem> buildMemberReport(String memberId) {
        return libraryService.getLoansForMember(memberId).stream()
                .filter(loan -> !loan.isReturned())
                .map(loan -> {
                    BookCopy copy = copies.findById(loan.getCopyId()).orElse(null);
                    Book book = copy == null ? null : books.findById(copy.getIsbn()).orElse(null);
                    return new MemberLoanReportItem(
                            loan.getLoanId(),
                            book == null ? "-" : book.getIsbn(),
                            book == null ? "-" : book.getTitle(),
                            loan.getCopyId(),
                            loan.getLoanDate(),
                            loan.getDueDate()
                    );
                })
                .collect(Collectors.toList());
    }

    public List<PopularBookItem> buildPopularBooksReport(int limit) {
        List<Book> all = books.findAll();
        return all.stream()
                .sorted(Comparator.comparingInt(Book::getTotalLoans).reversed())
                .limit(limit)
                .map(book -> new PopularBookItem(book.getIsbn(), book.getTitle(), book.getAuthor(), book.getTotalLoans()))
                .collect(Collectors.toList());
    }
}