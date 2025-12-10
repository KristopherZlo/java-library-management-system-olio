package lms.storage.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lms.exception.StorageException;
import lms.model.Book;
import lms.storage.Repository;

public class SqliteBookRepository implements Repository<Book, String> {
    private final SqliteStorage storage;

    public SqliteBookRepository(SqliteStorage storage) {
        this.storage = storage;
    }

    @Override
    public void save(Book book) {
        String sql = "INSERT OR REPLACE INTO books(isbn, book_id, title, author, year, genre, total_loans) " +
                "VALUES(?,?,?,?,?,?,?)";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, book.getIsbn());
                statement.setString(2, book.getBookId());
                statement.setString(3, book.getTitle());
                statement.setString(4, book.getAuthor());
                statement.setInt(5, book.getYear());
                statement.setString(6, book.getGenre());
                statement.setInt(7, book.getTotalLoans());
                statement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to save book " + book.getIsbn(), ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    @Override
    public Optional<Book> findById(String isbn) {
        String sql = "SELECT isbn, book_id, title, author, year, genre, total_loans FROM books WHERE isbn = ?";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, isbn);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to load book " + isbn, ex);
        } finally {
            storage.closeConnection(connection);
        }
        return Optional.empty();
    }

    @Override
    public List<Book> findAll() {
        String sql = "SELECT isbn, book_id, title, author, year, genre, total_loans FROM books";
        List<Book> books = new ArrayList<>();
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to list books", ex);
        } finally {
            storage.closeConnection(connection);
        }
        return books;
    }

    @Override
    public void deleteById(String isbn) {
        String sql = "DELETE FROM books WHERE isbn = ?";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, isbn);
                statement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to delete book " + isbn, ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    @Override
    public boolean existsById(String isbn) {
        String sql = "SELECT 1 FROM books WHERE isbn = ?";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, isbn);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to check book " + isbn, ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setIsbn(rs.getString("isbn"));
        book.setBookId(rs.getString("book_id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setYear(rs.getInt("year"));
        book.setGenre(rs.getString("genre"));
        book.setTotalLoans(rs.getInt("total_loans"));
        return book;
    }
}