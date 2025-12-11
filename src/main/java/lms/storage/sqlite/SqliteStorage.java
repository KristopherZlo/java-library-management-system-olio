package lms.storage.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;
import lms.exception.StorageException;
import lms.model.Book;
import lms.model.BookCopy;
import lms.model.Loan;
import lms.model.Member;
import lms.model.Reservation;
import lms.storage.LibraryStorage;
import lms.storage.Repository;

public class SqliteStorage implements LibraryStorage {
    private final String url;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();
    private final Repository<Book, String> bookRepository;
    private final Repository<BookCopy, String> copyRepository;
    private final Repository<Member, String> memberRepository;
    private final Repository<Loan, String> loanRepository;
    private final Repository<Reservation, String> reservationRepository;

    public SqliteStorage(String dataDir) {
        try {
            Path base = Paths.get(dataDir);
            Files.createDirectories(base);
            this.url = "jdbc:sqlite:" + base.resolve("lms.db");
            initSchema();
        } catch (Exception ex) {
            throw new StorageException("Failed to initialize SQLite storage", ex);
        }
        this.bookRepository = new SqliteBookRepository(this);
        this.copyRepository = new SqliteCopyRepository(this);
        this.memberRepository = new SqliteMemberRepository(this);
        this.loanRepository = new SqliteLoanRepository(this);
        this.reservationRepository = new SqliteReservationRepository(this);
    }

    Connection connect() throws SQLException {
        Connection existing = transactionConnection.get();
        if (existing != null) {
            return existing;
        }
        return DriverManager.getConnection(url);
    }

    void closeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        if (transactionConnection.get() == connection) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // Ignore close failures for standalone connections.
        }
    }

    private void initSchema() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "isbn TEXT PRIMARY KEY," +
                    "book_id TEXT," +
                    "title TEXT NOT NULL," +
                    "author TEXT NOT NULL," +
                    "year INTEGER," +
                    "genre TEXT," +
                    "total_loans INTEGER DEFAULT 0" +
                    ")");
            try {
                statement.execute("ALTER TABLE books ADD COLUMN book_id TEXT");
            } catch (SQLException ignored) {
                // Column exists or table not empty; ignore.
            }
            statement.execute("CREATE TABLE IF NOT EXISTS copies (" +
                    "copy_id TEXT PRIMARY KEY," +
                    "isbn TEXT NOT NULL," +
                    "status TEXT NOT NULL," +
                    "FOREIGN KEY(isbn) REFERENCES books(isbn)" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS members (" +
                    "member_id TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "email TEXT NOT NULL," +
                    "type TEXT NOT NULL" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS loans (" +
                    "loan_id TEXT PRIMARY KEY," +
                    "copy_id TEXT NOT NULL," +
                    "member_id TEXT NOT NULL," +
                    "loan_date TEXT NOT NULL," +
                    "due_date TEXT NOT NULL," +
                    "return_date TEXT," +
                    "FOREIGN KEY(copy_id) REFERENCES copies(copy_id)," +
                    "FOREIGN KEY(member_id) REFERENCES members(member_id)" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                    "res_id TEXT PRIMARY KEY," +
                    "isbn TEXT NOT NULL," +
                    "member_id TEXT NOT NULL," +
                    "created_at TEXT NOT NULL," +
                    "status TEXT NOT NULL," +
                    "FOREIGN KEY(isbn) REFERENCES books(isbn)," +
                    "FOREIGN KEY(member_id) REFERENCES members(member_id)" +
                    ")");
        }
    }

    @Override
    public Repository<Book, String> books() {
        return bookRepository;
    }

    @Override
    public Repository<BookCopy, String> copies() {
        return copyRepository;
    }

    @Override
    public Repository<Member, String> members() {
        return memberRepository;
    }

    @Override
    public Repository<Loan, String> loans() {
        return loanRepository;
    }

    @Override
    public Repository<Reservation, String> reservations() {
        return reservationRepository;
    }

    @Override
    public <T> T runInTransaction(Supplier<T> action) {
        if (transactionConnection.get() != null) {
            return action.get();
        }
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
            T result = action.get();
            connection.commit();
            return result;
        } catch (RuntimeException | Error ex) {
            rollbackQuietly(connection);
            throw ex;
        } catch (SQLException ex) {
            rollbackQuietly(connection);
            throw new StorageException("Failed to execute SQLite transaction", ex);
        } finally {
            transactionConnection.remove();
            closeConnection(connection);
        }
    }

    @Override
    public void close() {
        // SQLite connections are per-operation.
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Ignore rollback failures.
        }
    }
}