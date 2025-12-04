# Library Management System (TUI)

## Purpose
This project was created for educational purposes. During development, AI tools were used in some parts to draft text
and to fix critical bugs.
AI was used for text drafts and bug-fixing suggestions; all architecture decisions, domain model, and final code were
implemented and reviewed by me.

## Features
- Lanterna-based TUI with lists, selection, and guided prompts
- CRUD for books, members, and reservations; copy management for books
- Loan and return workflows with member-based loan policies
- Update loan dates and due dates
- Reservation queue with READY/QUEUED/FULFILLED/CANCELLED states
- Search for books and members using n-gram fuzzy matching (2-3 grams)
- Reports: overdue loans, member loans, popular books, CSV export
- SQLite storage with JSON file fallback and transactional operations
- Configurable storage mode, demo data toggle, and app logging
- JUnit tests for the service layer

## Domain model
- Book(bookId, isbn, title, author, year, genre, totalLoans)
- BookCopy(copyId, isbn, status)
- Member(memberId, name, email, type)
- Loan(loanId, copyId, memberId, loanDate, dueDate, returnDate)
- Reservation(reservationId, isbn, memberId, createdAt, status)

Statuses:
- CopyStatus: AVAILABLE, LOANED, RESERVED, LOST
- ReservationStatus: QUEUED, READY, FULFILLED, CANCELLED
- MemberType: STUDENT, ADULT

## Loan / Return / Reserve protocol
Loan:
1) Validate member and book
2) Check maxLoans via LoanPolicy
3) If member has a READY reservation, use a RESERVED copy
4) Otherwise pick an AVAILABLE copy; if none, inform the user to reserve
5) Create Loan, set copy to LOANED, increment book totals, fulfill reservation if used

Return:
1) Find active loan by copy ID
2) Set returnDate on the loan
3) Set copy to AVAILABLE
4) If queued reservations exist, mark next as READY and set copy to RESERVED

Reserve:
1) Validate member and book
2) Ensure no active reservation for the same member and book
3) Ensure no AVAILABLE copies
4) Create a QUEUED reservation

## Architecture and packages
- lms.ui: Lanterna windows (TUI)
- lms.model: entities and enums
- lms.service: business logic (loan/return/reserve, search)
- lms.storage: repositories and storage factory
- lms.storage.sqlite: SQLite/JDBC repositories
- lms.storage.file: JSON file repositories
- lms.report: reports and CSV export
- lms.policy: loan policies and fine policies
- lms.util: validation, ID generation, date providers, config, logging
- lms.exception: custom exceptions

## Learning outcomes / OOP coverage
- Classes/objects: lms.model.Book, lms.model.Loan, lms.model.Reservation, lms.model.Member
- Encapsulation & validation: lms.util.Validators, lms.exception.ValidationException
- Inheritance: lms.model.User -> lms.model.Member, lms.model.Librarian
- Polymorphism: LoanPolicy implementations, FinePolicy implementations, StorageFactory selects storage
- Interfaces & abstraction: Repository<T, ID>, LibraryStorage, LoanPolicy, FinePolicy
- Exceptions: lms.exception.* and UI handling in lms.ui.DialogUtils
- Collections: List/Set/Queue in LibraryService, Map/EnumMap in LoanPolicyResolver
- Generics: Repository<T, ID>, DialogUtils.Choice<T>
- File I/O: JSON repositories in lms.storage.file, CSV export in lms.report.CsvExporter
- DB access: lms.storage.sqlite.* via JDBC
- Testing: JUnit 5 in src/test/java/lms/service/LibraryServiceTest.java

## Storage and configuration
- SQLite database: `data/lms.db`
- JSON fallback: `data/books.json`, `data/copies.json`, etc.
- Config file: `config/app.properties`
  - `storage.mode=SQLITE` or `storage.mode=FILE`
  - `demo.enabled=true` or `demo.enabled=false`
- If SQLite initialization fails (JDBC or connection errors), the app falls back to file storage
- Logs are written to `app.log`

## Transactional operations (examples)
- LibraryStorage.runInTransaction keeps related updates atomic
- LibraryService.loanByIsbn: set copy LOANED, create loan, increment book totals, fulfill reservation
- LibraryService.returnByCopyId: set returnDate, update copy status, promote next reservation to READY
- LibraryService.removeBook: delete related copies and the book in one transaction

## Running
Requirements: Java 11, Maven.

Run the TUI:
```
mvn exec:java
```

Run tests:
```
mvn test
```

Build a runnable JAR:
```
mvn package
```
The shaded JAR is created under `target/*-all.jar`.

## TUI usage
- Tab / Shift+Tab: move focus between controls
- Arrow keys: navigate tables and lists
- Enter: activate buttons or select rows
- F1: open help
- Selected rows are marked with `*`

## Demo data
- Demo data is seeded only when storage is empty
- Disable it with `demo.enabled=false` in `config/app.properties`

## How to grade / what to demo
- Add book and add copies
- Loan until maxLoans triggers an error
- Return triggers reservation READY
- Overdue report and export CSV
- Switch storage mode and restart
- Search books/members with short and long queries

## Resources
General:
- Integrated Library System - overview (Wikipedia). Used for ILS terminology and scope.
- The Integrated Library System (ILS) Primer (Lucidea). Used for ILS concepts and workflows.

Architecture and real-world solutions (most important):
- Koha - official documentation (Koha). Used as reference for reservation queues and circulation flow.
- FOLIO documentation (overview) (FOLIO). Used for modular architecture and ILS vocabulary.

How to design your own LMS (applied):
- Library Management System - Software Engineering perspective (GeeksForGeeks). Used for feature checklist and use cases.
- Building a Library Management System (Python / OOP) (Medium). Used for OOP modeling ideas.