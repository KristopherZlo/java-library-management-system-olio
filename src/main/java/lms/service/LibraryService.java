package lms.service;
// TODO: refine workflows
// TODO: add metrics hooks

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import lms.exception.NotFoundException;
import lms.exception.RuleViolationException;
import lms.exception.ValidationException;
import lms.model.Book;
import lms.model.BookCopy;
import lms.model.CopyStatus;
import lms.model.Loan;
import lms.model.Member;
import lms.model.MemberType;
import lms.model.Reservation;
import lms.model.ReservationStatus;
import lms.policy.FinePolicy;
import lms.policy.LoanPolicy;
import lms.policy.LoanPolicyResolver;
import lms.storage.LibraryStorage;
import lms.storage.Repository;
import lms.util.DateProvider;
import lms.util.IdGenerator;
import lms.util.Validators;

public class LibraryService {
    private static final double SHORT_THRESHOLD = 0.12;
    private static final int FALLBACK_LIMIT = 10;
    private final LibraryStorage storage;
    private final Repository<Book, String> books;
    private final Repository<BookCopy, String> copies;
    private final Repository<Member, String> members;
    private final Repository<Loan, String> loans;
    private final Repository<Reservation, String> reservations;
    private final LoanPolicyResolver loanPolicyResolver;
    private final FinePolicy finePolicy;
    private final DateProvider dateProvider;

    public LibraryService(LibraryStorage storage,
                          LoanPolicyResolver loanPolicyResolver,
                          FinePolicy finePolicy,
                          DateProvider dateProvider) {
        this.storage = storage;
        this.books = storage.books();
        this.copies = storage.copies();
        this.members = storage.members();
        this.loans = storage.loans();
        this.reservations = storage.reservations();
        this.loanPolicyResolver = loanPolicyResolver;
        this.finePolicy = finePolicy;
        this.dateProvider = dateProvider;
    }

    public void addBook(Book book) {
        if (book == null) {
            throw new ValidationException("Book is required");
        }
        String isbn = Validators.validateIsbn(book.getIsbn());
        if (books.existsById(isbn)) {
            throw new RuleViolationException("Book already exists: " + isbn);
        }
        String bookId = book.getBookId();
        if (bookId == null || bookId.trim().isEmpty()) {
            bookId = IdGenerator.newId("BOOK");
        } else {
            bookId = bookId.trim();
        }
        book.setBookId(bookId);
        book.setIsbn(isbn);
        book.setTitle(Validators.requireNonBlank(book.getTitle(), "Title"));
        book.setAuthor(Validators.requireNonBlank(book.getAuthor(), "Author"));
        book.setGenre(Validators.requireNonBlank(book.getGenre(), "Genre"));
        if (book.getYear() <= 0) {
            throw new ValidationException("Year must be positive");
        }
        books.save(book);
    }

    public void addBook(Book book, int copiesCount) {
        if (copiesCount < 0) {
            throw new ValidationException("Copies count must be non-negative");
        }
        storage.runInTransaction(() -> {
            addBook(book);
            for (int i = 0; i < copiesCount; i++) {
                addCopy(book.getIsbn());
            }
            return null;
        });
    }

    public BookCopy addCopy(String isbn) {
        String cleaned = resolveIsbn(isbn);
        Book book = books.findById(cleaned)
                .orElseThrow(() -> new NotFoundException("Book not found: " + isbn));
        BookCopy copy = new BookCopy(IdGenerator.newId("COPY"), book.getIsbn(), CopyStatus.AVAILABLE);
        copies.save(copy);
        return copy;
    }

    public void markCopyLost(String copyId) {
        BookCopy copy = copies.findById(copyId)
                .orElseThrow(() -> new NotFoundException("Copy not found: " + copyId));
        copy.setStatus(CopyStatus.LOST);
        copies.save(copy);
    }

    public void removeBook(String isbn) {
        storage.runInTransaction(() -> {
            String cleaned = resolveIsbn(isbn);
            List<BookCopy> relatedCopies = copies.findAll().stream()
                    .filter(copy -> cleaned.equals(copy.getIsbn()))
                    .collect(Collectors.toList());
            Set<String> copyIds = relatedCopies.stream()
                    .map(BookCopy::getCopyId)
                    .collect(Collectors.toSet());
            boolean hasLoanHistory = loans.findAll().stream()
                    .anyMatch(loan -> copyIds.contains(loan.getCopyId()));
            if (hasLoanHistory) {
                throw new RuleViolationException("Cannot remove book with loan history");
            }
            boolean hasReservationHistory = reservations.findAll().stream()
                    .anyMatch(res -> cleaned.equals(res.getIsbn()));
            if (hasReservationHistory) {
                throw new RuleViolationException("Cannot remove book with reservations");
            }
            for (BookCopy copy : relatedCopies) {
                copies.deleteById(copy.getCopyId());
            }
            books.deleteById(cleaned);
            return null;
        });
    }

    public List<Book> listBooks() {
        return books.findAll();
    }

    public Book updateBook(String isbnOrBookId, String bookId, String title, String author, int year, String genre) {
        String cleanedIsbn = resolveIsbn(isbnOrBookId);
        Book book = books.findById(cleanedIsbn)
                .orElseThrow(() -> new NotFoundException("Book not found: " + isbnOrBookId));
        book.setTitle(Validators.requireNonBlank(title, "Title"));
        book.setAuthor(Validators.requireNonBlank(author, "Author"));
        book.setGenre(Validators.requireNonBlank(genre, "Genre"));
        if (year <= 0) {
            throw new ValidationException("Year must be positive");
        }
        book.setYear(year);
        if (bookId != null) {
            String trimmed = bookId.trim();
            book.setBookId(trimmed.isEmpty() ? null : trimmed);
        }
        books.save(book);
        return book;
    }

    public List<Book> searchBooks(String query) {
        String q = Validators.requireNonBlank(query, "Query");
        List<Book> allBooks = books.findAll();
        return fuzzySearch(allBooks, q);
    }

    public List<Book> searchBooks(String title, String author) {
        String titleQuery = title == null ? "" : title.trim().toLowerCase();
        String authorQuery = author == null ? "" : author.trim().toLowerCase();
        return books.findAll().stream()
                .filter(book -> book.getTitle().toLowerCase().contains(titleQuery))
                .filter(book -> book.getAuthor().toLowerCase().contains(authorQuery))
                .collect(Collectors.toList());
    }

    public List<BookCopy> getCopiesByIsbn(String isbn) {
        String cleaned = resolveIsbn(isbn);
        return copies.findAll().stream()
                .filter(copy -> cleaned.equals(copy.getIsbn()))
                .collect(Collectors.toList());
    }

    public Member addMember(Member member) {
        if (member == null) {
            throw new ValidationException("Member is required");
        }
        String id = member.getMemberId();
        if (id == null || id.trim().isEmpty()) {
            id = IdGenerator.newId("MEM");
            member.setMemberId(id);
        }
        member.setName(Validators.requireNonBlank(member.getName(), "Name"));
        member.setEmail(Validators.validateEmail(member.getEmail()));
        if (member.getType() == null) {
            throw new ValidationException("Member type is required");
        }
        if (members.existsById(id)) {
            throw new RuleViolationException("Member already exists: " + id);
        }
        members.save(member);
        return member;
    }

    public Member updateMember(String memberId, String name, String email, MemberType type) {
        String cleaned = Validators.requireNonBlank(memberId, "Member ID");
        Member member = members.findById(cleaned)
                .orElseThrow(() -> new NotFoundException("Member not found: " + cleaned));
        member.setName(Validators.requireNonBlank(name, "Name"));
        member.setEmail(Validators.validateEmail(email));
        if (type == null) {
            throw new ValidationException("Member type is required");
        }
        member.setType(type);
        members.save(member);
        return member;
    }

    public List<Member> listMembers() {
        return members.findAll();
    }

    public void removeMember(String memberId) {
        String cleaned = Validators.requireNonBlank(memberId, "Member ID");
        if (!members.existsById(cleaned)) {
            throw new NotFoundException("Member not found: " + cleaned);
        }
        boolean hasActiveLoan = loans.findAll().stream()
                .anyMatch(loan -> cleaned.equals(loan.getMemberId()) && !loan.isReturned());
        if (hasActiveLoan) {
            throw new RuleViolationException("Cannot remove member with active loans");
        }
        boolean hasActiveReservation = reservations.findAll().stream()
                .anyMatch(res -> cleaned.equals(res.getMemberId()) && res.isActive());
        if (hasActiveReservation) {
            throw new RuleViolationException("Cannot remove member with active reservations");
        }
        members.deleteById(cleaned);
    }

    public Loan loanByIsbn(String isbn, String memberId) {
        return loanByIsbn(isbn, memberId, null, null);
    }

    public Loan loanByIsbn(String isbn, String memberId, LocalDate loanDate, LocalDate dueDate) {
        return storage.runInTransaction(() -> {
            String cleanedIsbn = resolveIsbn(isbn);
            String cleanedMemberId = Validators.requireNonBlank(memberId, "Member ID");
            Member member = members.findById(cleanedMemberId)
                    .orElseThrow(() -> new NotFoundException("Member not found: " + cleanedMemberId));
            Book book = books.findById(cleanedIsbn)
                    .orElseThrow(() -> new NotFoundException("Book not found: " + cleanedIsbn));

            LoanPolicy policy = loanPolicyResolver.forMember(member);
            long activeLoans = loans.findAll().stream()
                    .filter(loan -> cleanedMemberId.equals(loan.getMemberId()) && !loan.isReturned())
                    .count();
            if (activeLoans >= policy.maxLoans(member)) {
                throw new RuleViolationException("Member has reached loan limit");
            }

            LocalDate resolvedLoanDate = loanDate == null ? dateProvider.today() : loanDate;
            LocalDate resolvedDueDate;
            if (dueDate != null) {
                if (dueDate.isBefore(resolvedLoanDate)) {
                    throw new ValidationException("Due date must be on or after loan date");
                }
                resolvedDueDate = dueDate;
            } else {
                resolvedDueDate = resolvedLoanDate.plusDays(policy.loanDays(member));
            }

            Optional<Reservation> readyReservation = reservations.findAll().stream()
                    .filter(res -> cleanedIsbn.equals(res.getIsbn()))
                    .filter(res -> res.getStatus() == ReservationStatus.READY)
                    .filter(res -> cleanedMemberId.equals(res.getMemberId()))
                    .findFirst();
            if (readyReservation.isPresent()) {
                BookCopy reservedCopy = findFirstCopy(cleanedIsbn, CopyStatus.RESERVED)
                        .orElseThrow(() -> new RuleViolationException("Reserved copy not found"));
                return finalizeLoan(book, member, reservedCopy, readyReservation.get(),
                        resolvedLoanDate, resolvedDueDate);
            }

            BookCopy availableCopy = findFirstCopy(cleanedIsbn, CopyStatus.AVAILABLE)
                    .orElseThrow(() -> new RuleViolationException("No available copy, consider reservation"));
            return finalizeLoan(book, member, availableCopy, null, resolvedLoanDate, resolvedDueDate);
        });
    }

    public ReturnResult returnByCopyId(String copyId) {
        return storage.runInTransaction(() -> {
            String cleaned = Validators.requireNonBlank(copyId, "Copy ID");
            BookCopy copy = copies.findById(cleaned)
                    .orElseThrow(() -> new NotFoundException("Copy not found: " + cleaned));
            Loan loan = loans.findAll().stream()
                    .filter(existing -> cleaned.equals(existing.getCopyId()) && !existing.isReturned())
                    .findFirst()
                    .orElseThrow(() -> new RuleViolationException("Active loan not found for copy"));
            loan.setReturnDate(dateProvider.today());
            loans.save(loan);

            copy.setStatus(CopyStatus.AVAILABLE);
            Reservation nextReservation = promoteNextReservation(copy.getIsbn());
            if (nextReservation != null) {
                copy.setStatus(CopyStatus.RESERVED);
            }
            copies.save(copy);
            return new ReturnResult(loan, nextReservation, nextReservation == null ? null : copy);
        });
    }

    public Reservation reserveByIsbn(String isbn, String memberId) {
        String cleanedIsbn = resolveIsbn(isbn);
        String cleanedMemberId = Validators.requireNonBlank(memberId, "Member ID");
        if (!members.existsById(cleanedMemberId)) {
            throw new NotFoundException("Member not found: " + cleanedMemberId);
        }
        boolean alreadyReserved = reservations.findAll().stream()
                .anyMatch(res -> cleanedIsbn.equals(res.getIsbn())
                        && cleanedMemberId.equals(res.getMemberId())
                        && res.isActive());
        if (alreadyReserved) {
            throw new RuleViolationException("Member already has a reservation for this book");
        }
        boolean anyAvailable = findFirstCopy(cleanedIsbn, CopyStatus.AVAILABLE).isPresent();
        if (anyAvailable) {
            throw new RuleViolationException("Copy is available, use loan instead of reservation");
        }
        Reservation reservation = new Reservation(
                IdGenerator.newId("RES"),
                cleanedIsbn,
                cleanedMemberId,
                dateProvider.today(),
                ReservationStatus.QUEUED
        );
        reservations.save(reservation);
        return reservation;
    }

    public Reservation updateReservation(String reservationId, String memberId, ReservationStatus status) {
        String cleaned = Validators.requireNonBlank(reservationId, "Reservation ID");
        return storage.runInTransaction(() -> {
            Reservation reservation = reservations.findById(cleaned)
                    .orElseThrow(() -> new NotFoundException("Reservation not found: " + cleaned));
            String updatedMemberId = memberId == null ? reservation.getMemberId()
                    : Validators.requireNonBlank(memberId, "Member ID");
            if (!members.existsById(updatedMemberId)) {
                throw new NotFoundException("Member not found: " + updatedMemberId);
            }
            ReservationStatus newStatus = status == null ? reservation.getStatus() : status;
            ReservationStatus oldStatus = reservation.getStatus();
            if (oldStatus == ReservationStatus.READY && newStatus == ReservationStatus.QUEUED) {
                throw new RuleViolationException("Cannot move READY reservation back to QUEUED");
            }
            Optional<BookCopy> availableCopy = Optional.empty();
            if (newStatus == ReservationStatus.READY && oldStatus != ReservationStatus.READY) {
                boolean otherReady = reservations.findAll().stream()
                        .filter(res -> reservation.getIsbn().equals(res.getIsbn()))
                        .filter(res -> res.getStatus() == ReservationStatus.READY)
                        .anyMatch(res -> !res.getReservationId().equals(reservation.getReservationId()));
                if (otherReady) {
                    throw new RuleViolationException("Another reservation is already READY for this book");
                }
                availableCopy = findFirstCopy(reservation.getIsbn(), CopyStatus.AVAILABLE);
                if (availableCopy.isEmpty()) {
                    throw new RuleViolationException("No available copy to reserve");
                }
            }

            reservation.setMemberId(updatedMemberId);
            reservation.setStatus(newStatus);
            reservations.save(reservation);

            if (newStatus == ReservationStatus.READY && oldStatus != ReservationStatus.READY) {
                BookCopy copy = availableCopy.get();
                copy.setStatus(CopyStatus.RESERVED);
                copies.save(copy);
            }

            if (oldStatus == ReservationStatus.READY && newStatus != ReservationStatus.READY) {
                Optional<BookCopy> reservedCopy = findFirstCopy(reservation.getIsbn(), CopyStatus.RESERVED);
                Reservation next = promoteNextReservation(reservation.getIsbn());
                if (next == null) {
                    if (reservedCopy.isPresent()) {
                        BookCopy copy = reservedCopy.get();
                        copy.setStatus(CopyStatus.AVAILABLE);
                        copies.save(copy);
                    }
                } else if (reservedCopy.isEmpty()) {
                    Optional<BookCopy> fallback = findFirstCopy(reservation.getIsbn(), CopyStatus.AVAILABLE);
                    if (fallback.isEmpty()) {
                        throw new RuleViolationException("No copy available to reserve for next reservation");
                    }
                    BookCopy copy = fallback.get();
                    copy.setStatus(CopyStatus.RESERVED);
                    copies.save(copy);
                }
            }
            return reservation;
        });
    }

    public List<Loan> getActiveLoans() {
        return loans.findAll().stream()
                .filter(loan -> !loan.isReturned())
                .collect(Collectors.toList());
    }

    public List<Loan> getOverdueLoans(LocalDate today) {
        LocalDate date = today == null ? dateProvider.today() : today;
        return loans.findAll().stream()
                .filter(loan -> loan.isOverdue(date))
                .collect(Collectors.toList());
    }

    public List<Loan> getLoansForMember(String memberId) {
        String cleanedMemberId = Validators.requireNonBlank(memberId, "Member ID");
        return loans.findAll().stream()
                .filter(loan -> cleanedMemberId.equals(loan.getMemberId()))
                .collect(Collectors.toList());
    }

    public LocalDate suggestDueDate(String memberId, LocalDate loanDate) {
        String cleanedMemberId = Validators.requireNonBlank(memberId, "Member ID");
        Member member = members.findById(cleanedMemberId)
                .orElseThrow(() -> new NotFoundException("Member not found: " + cleanedMemberId));
        LocalDate resolvedLoanDate = loanDate == null ? dateProvider.today() : loanDate;
        LoanPolicy policy = loanPolicyResolver.forMember(member);
        return resolvedLoanDate.plusDays(policy.loanDays(member));
    }

    public Loan updateLoanDate(String loanId, LocalDate newLoanDate) {
        String cleaned = Validators.requireNonBlank(loanId, "Loan ID");
        if (newLoanDate == null) {
            throw new ValidationException("Loan date is required");
        }
        return storage.runInTransaction(() -> {
            Loan loan = loans.findById(cleaned)
                    .orElseThrow(() -> new NotFoundException("Loan not found: " + cleaned));
            LocalDate oldLoanDate = loan.getLoanDate();
            LocalDate oldDueDate = loan.getDueDate();
            loan.setLoanDate(newLoanDate);
            LocalDate updatedDueDate = calculateDueDateForUpdatedLoan(loan, oldLoanDate, oldDueDate);
            if (updatedDueDate != null) {
                loan.setDueDate(updatedDueDate);
            }
            loans.save(loan);
            return loan;
        });
    }

    public Loan updateLoanDueDate(String loanId, LocalDate newDueDate) {
        String cleaned = Validators.requireNonBlank(loanId, "Loan ID");
        if (newDueDate == null) {
            throw new ValidationException("Due date is required");
        }
        return storage.runInTransaction(() -> {
            Loan loan = loans.findById(cleaned)
                    .orElseThrow(() -> new NotFoundException("Loan not found: " + cleaned));
            if (loan.getLoanDate() != null && newDueDate.isBefore(loan.getLoanDate())) {
                throw new ValidationException("Due date must be on or after loan date");
            }
            loan.setDueDate(newDueDate);
            loans.save(loan);
            return loan;
        });
    }

    public List<Reservation> getReservationsForIsbn(String isbn) {
        String cleanedIsbn = resolveIsbn(isbn);
        return reservations.findAll().stream()
                .filter(res -> cleanedIsbn.equals(res.getIsbn()))
                .collect(Collectors.toList());
    }

    public Book getBookByKey(String isbnOrBookId) {
        String cleanedIsbn = resolveIsbn(isbnOrBookId);
        return books.findById(cleanedIsbn)
                .orElseThrow(() -> new NotFoundException("Book not found: " + isbnOrBookId));
    }

    public List<Reservation> listReservations() {
        return reservations.findAll();
    }

    public List<Member> searchMembers(String query) {
        String q = Validators.requireNonBlank(query, "Query");
        List<Member> allMembers = members.findAll();
        return fuzzySearchMembers(allMembers, q);
    }

    public void removeReservation(String reservationId) {
        String cleaned = Validators.requireNonBlank(reservationId, "Reservation ID");
        storage.runInTransaction(() -> {
            Reservation reservation = reservations.findById(cleaned)
                    .orElseThrow(() -> new NotFoundException("Reservation not found: " + cleaned));
            reservations.deleteById(cleaned);
            if (reservation.getStatus() == ReservationStatus.READY) {
                Optional<BookCopy> reservedCopy = findFirstCopy(reservation.getIsbn(), CopyStatus.RESERVED);
                Reservation next = promoteNextReservation(reservation.getIsbn());
                if (next == null && reservedCopy.isPresent()) {
                    BookCopy copy = reservedCopy.get();
                    copy.setStatus(CopyStatus.AVAILABLE);
                    copies.save(copy);
                }
            }
            return null;
        });
    }

    public long calculateFineCents(Loan loan, LocalDate today) {
        return finePolicy.fineCents(loan, today == null ? dateProvider.today() : today);
    }

    private Loan finalizeLoan(Book book, Member member, BookCopy copy, Reservation reservation,
                              LocalDate loanDate, LocalDate dueDate) {
        copy.setStatus(CopyStatus.LOANED);
        copies.save(copy);
        Loan loan = new Loan(IdGenerator.newId("LOAN"), copy.getCopyId(), member.getMemberId(),
                loanDate, dueDate);
        loans.save(loan);

        book.incrementTotalLoans();
        books.save(book);

        if (reservation != null) {
            reservation.setStatus(ReservationStatus.FULFILLED);
            reservations.save(reservation);
        }
        return loan;
    }

    private Optional<BookCopy> findFirstCopy(String isbn, CopyStatus status) {
        return copies.findAll().stream()
                .filter(copy -> isbn.equals(copy.getIsbn()))
                .filter(copy -> copy.getStatus() == status)
                .findFirst();
    }

    private Reservation promoteNextReservation(String isbn) {
        boolean hasReady = reservations.findAll().stream()
                .anyMatch(res -> isbn.equals(res.getIsbn()) && res.getStatus() == ReservationStatus.READY);
        if (hasReady) {
            return null;
        }
        Queue<Reservation> queue = buildReservationQueue(isbn);
        Reservation next = queue.poll();
        if (next == null) {
            return null;
        }
        next.setStatus(ReservationStatus.READY);
        reservations.save(next);
        return next;
    }

    private Queue<Reservation> buildReservationQueue(String isbn) {
        List<Reservation> queued = reservations.findAll().stream()
                .filter(res -> isbn.equals(res.getIsbn()))
                .filter(res -> res.getStatus() == ReservationStatus.QUEUED)
                .sorted(Comparator.comparing(Reservation::getCreatedAt))
                .collect(Collectors.toList());
        return new ArrayDeque<>(queued);
    }

    private boolean matches(Book book, String query) {
        String q = query == null ? "" : query.toLowerCase();
        String bookId = book.getBookId();
        return book.getTitle().toLowerCase().contains(q)
                || book.getAuthor().toLowerCase().contains(q)
                || book.getGenre().toLowerCase().contains(q)
                || book.getIsbn().toLowerCase().contains(q)
                || (bookId != null && bookId.toLowerCase().contains(q));
    }

    private String resolveIsbn(String isbnOrBookId) {
        String value = Validators.requireNonBlank(isbnOrBookId, "ISBN or Book ID");
        String trimmed = value.trim();
        String digits = trimmed.replace("-", "");
        if (digits.matches("\\d{10}|\\d{13}")) {
            String normalized = Validators.validateIsbn(trimmed);
            if (books.existsById(normalized)) {
                return normalized;
            }
        }
        Optional<Book> byBookId = books.findAll().stream()
                .filter(book -> book.getBookId() != null)
                .filter(book -> book.getBookId().equalsIgnoreCase(trimmed))
                .findFirst();
        if (byBookId.isPresent()) {
            return byBookId.get().getIsbn();
        }
        throw new NotFoundException("Book not found: " + trimmed);
    }

    private boolean matches(Member member, String query) {
        String q = query == null ? "" : query.toLowerCase();
        String memberId = member.getMemberId();
        return member.getName().toLowerCase().contains(q)
                || (memberId != null && memberId.toLowerCase().contains(q))
                || (member.getEmail() != null && member.getEmail().toLowerCase().contains(q));
    }

    private List<Book> fuzzySearch(List<Book> allBooks, String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.length() < 2) {
            return allBooks.stream()
                    .filter(book -> matches(book, query))
                    .sorted(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
        boolean useTrigrams = normalizedQuery.length() > SHORT_QUERY_MAX;
        double threshold = useTrigrams ? BASE_THRESHOLD : SHORT_THRESHOLD;
        List<BookScore> scored = new ArrayList<>();
        for (Book book : allBooks) {
            double score = similarityScore(query, normalizedQuery, book, useTrigrams);
            if (score > 0.0) {
                scored.add(new BookScore(book, score));
            }
        }
        scored.sort(Comparator.comparingDouble(BookScore::getScore).reversed()
                .thenComparing(score -> score.getBook().getTitle(), String.CASE_INSENSITIVE_ORDER));
        List<BookScore> closeMatches = scored.stream()
                .filter(score -> score.getScore() >= threshold)
                .collect(Collectors.toList());
        if (!closeMatches.isEmpty()) {
            return closeMatches.stream()
                    .map(BookScore::getBook)
                    .collect(Collectors.toList());
        }
        if (scored.isEmpty()) {
            return new ArrayList<>();
        }
        int limit = Math.min(FALLBACK_LIMIT, scored.size());
        return scored.stream()
                .limit(limit)
                .map(BookScore::getBook)
                .collect(Collectors.toList());
    }

    private List<Member> fuzzySearchMembers(List<Member> allMembers, String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.length() < 2) {
            return allMembers.stream()
                    .filter(member -> matches(member, query))
                    .sorted(Comparator.comparing(Member::getName, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(Member::getMemberId, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
        boolean useTrigrams = normalizedQuery.length() > SHORT_QUERY_MAX;
        double threshold = useTrigrams ? BASE_THRESHOLD : SHORT_THRESHOLD;
        List<MemberScore> scored = new ArrayList<>();
        for (Member member : allMembers) {
            double score = similarityScore(query, normalizedQuery, member, useTrigrams);
            if (score > 0.0) {
                scored.add(new MemberScore(member, score));
            }
        }
        scored.sort(Comparator.comparingDouble(MemberScore::getScore).reversed()
                .thenComparing(score -> score.getMember().getName(), String.CASE_INSENSITIVE_ORDER));
        List<MemberScore> closeMatches = scored.stream()
                .filter(score -> score.getScore() >= threshold)
                .collect(Collectors.toList());
        if (!closeMatches.isEmpty()) {
            return closeMatches.stream()
                    .map(MemberScore::getMember)
                    .collect(Collectors.toList());
        }
        if (scored.isEmpty()) {
            return new ArrayList<>();
        }
        int limit = Math.min(FALLBACK_LIMIT, scored.size());
        return scored.stream()
                .limit(limit)
                .map(MemberScore::getMember)
                .collect(Collectors.toList());
    }

    private LocalDate calculateDueDateForUpdatedLoan(Loan loan, LocalDate oldLoanDate, LocalDate oldDueDate) {
        if (oldLoanDate != null && oldDueDate != null) {
            long days = ChronoUnit.DAYS.between(oldLoanDate, oldDueDate);
            if (days > 0) {
                return loan.getLoanDate().plusDays(days);
            }
        }
        Member member = members.findById(loan.getMemberId()).orElse(null);
        if (member != null) {
            LoanPolicy policy = loanPolicyResolver.forMember(member);
            return loan.getLoanDate().plusDays(policy.loanDays(member));
        }
        return oldDueDate;
    }

    private double similarityScore(String rawQuery, String normalizedQuery, Book book, boolean useTrigrams) {
        if (matches(book, rawQuery)) {
            return 1.0;
        }
        return bestSimilarity(normalizedQuery, book, useTrigrams);
    }

    private double similarityScore(String rawQuery, String normalizedQuery, Member member, boolean useTrigrams) {
        if (matches(member, rawQuery)) {
            return 1.0;
        }
        return bestSimilarity(normalizedQuery, member, useTrigrams);
    }

    private double bestSimilarity(String normalizedQuery, Book book, boolean useTrigrams) {
        double best = 0.0;
        best = Math.max(best, bestFieldSimilarity(normalizedQuery, book.getTitle(), useTrigrams));
        best = Math.max(best, bestFieldSimilarity(normalizedQuery, book.getAuthor(), useTrigrams));
        best = Math.max(best, bestFieldSimilarity(normalizedQuery, book.getGenre(), useTrigrams));
        best = Math.max(best, bestFieldSimilarity(normalizedQuery, book.getIsbn(), useTrigrams));
        best = Math.max(best, bestFieldSimilarity(normalizedQuery, book.getBookId(), useTrigrams));
        return best;
    }

    private double bestSimilarity(String normalizedQuery, Member member, boolean useTrigrams) {
        double best = 0.0;
        best = Math.max(best, bestFieldSimilarity(normalizedQuery, member.getName(), useTrigrams));
        best = Math.max(best, bestFieldSimilarity(normalizedQuery, member.getMemberId(), useTrigrams));
        best = Math.max(best, bestFieldSimilarity(normalizedQuery, member.getEmail(), useTrigrams));
        return best;
    }

    private double bestFieldSimilarity(String normalizedQuery, String value, boolean useTrigrams) {
        if (normalizedQuery == null || normalizedQuery.isEmpty() || value == null) {
            return 0.0;
        }
        String normalizedValue = normalize(value);
        double best = ngramSimilarityNormalized(normalizedQuery, normalizedValue, useTrigrams);
        for (String token : tokenize(value)) {
            String normalizedToken = normalize(token);
            if (normalizedToken.isEmpty()) {
                continue;
            }
            double score = ngramSimilarityNormalized(normalizedQuery, normalizedToken, useTrigrams);
            score *= lengthPenalty(normalizedQuery.length(), normalizedToken.length());
            if (score > best) {
                best = score;
            }
        }
        return best;
    }

    private double ngramSimilarityNormalized(String normalizedQuery, String normalizedValue, boolean useTrigrams) {
        if (normalizedQuery == null || normalizedQuery.isEmpty() || normalizedValue == null || normalizedValue.isEmpty()) {
            return 0.0;
        }
        Set<String> gramsA = new HashSet<>();
        addNgrams(gramsA, normalizedQuery, 2);
        if (useTrigrams) {
            addNgrams(gramsA, normalizedQuery, 3);
        }
        Set<String> gramsB = new HashSet<>();
        addNgrams(gramsB, normalizedValue, 2);
        if (useTrigrams) {
            addNgrams(gramsB, normalizedValue, 3);
        }
        if (gramsA.isEmpty() || gramsB.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String gram : gramsA) {
            if (gramsB.contains(gram)) {
                intersection++;
            }
        }
        int union = gramsA.size() + gramsB.size() - intersection;
        if (union == 0) {
            return 0.0;
        }
        return (double) intersection / union;
    }

    private void addNgrams(Set<String> target, String value, int n) {
        if (value.length() < n) {
            return;
        }
        for (int i = 0; i <= value.length() - n; i++) {
            target.add(value.substring(i, i + n));
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase();
        StringBuilder builder = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private List<String> tokenize(String value) {
        String[] rawTokens = value.toLowerCase().split("[^a-z0-9]+");
        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private double lengthPenalty(int queryLength, int tokenLength) {
        if (queryLength <= 0 || tokenLength <= 0) {
            return 0.0;
        }
        int min = Math.min(queryLength, tokenLength);
        int max = Math.max(queryLength, tokenLength);
        double ratio = min / (double) max;
        return ratio * ratio;
    }

    private static class BookScore {
        private final Book book;
        private final double score;

        private BookScore(Book book, double score) {
            this.book = book;
            this.score = score;
        }

        public Book getBook() {
            return book;
        }

        public double getScore() {
            return score;
        }
    }

    private static class MemberScore {
        private final Member member;
        private final double score;

        private MemberScore(Member member, double score) {
            this.member = member;
            this.score = score;
        }

        public Member getMember() {
            return member;
        }

        public double getScore() {
            return score;
        }
    }
}