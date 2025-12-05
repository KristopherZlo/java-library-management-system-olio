package lms.storage.sqlite;
// TODO: tune statements
// TODO: add retry handling

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lms.exception.StorageException;
import lms.model.BookCopy;
import lms.model.CopyStatus;
import lms.storage.Repository;

public class SqliteCopyRepository implements Repository<BookCopy, String> {

    public SqliteCopyRepository(SqliteStorage storage) {
    }

    @Override
    public void save(BookCopy copy) {
        String sql = "INSERT OR REPLACE INTO copies(copy_id, isbn, status) VALUES(?,?,?)";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, copy.getCopyId());
                statement.setString(2, copy.getIsbn());
                statement.setString(3, copy.getStatus().name());
                statement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to save copy " + copy.getCopyId(), ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    @Override
    public Optional<BookCopy> findById(String id) {
        String sql = "SELECT copy_id, isbn, status FROM copies WHERE copy_id = ?";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to load copy " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
        return Optional.empty();
    }

    @Override
    public List<BookCopy> findAll() {
        String sql = "SELECT copy_id, isbn, status FROM copies";
        List<BookCopy> copies = new ArrayList<>();
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    copies.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to list copies", ex);
        } finally {
            storage.closeConnection(connection);
        }
        return copies;
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM copies WHERE copy_id = ?";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to delete copy " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM copies WHERE copy_id = ?";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to check copy " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    private BookCopy mapRow(ResultSet rs) throws SQLException {
        BookCopy copy = new BookCopy();
        copy.setCopyId(rs.getString("copy_id"));
        copy.setIsbn(rs.getString("isbn"));
        copy.setStatus(CopyStatus.valueOf(rs.getString("status")));
        return copy;
    }
}