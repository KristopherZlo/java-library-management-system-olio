package lms.storage.sqlite;
// TODO: tune statements
// TODO: add retry handling

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lms.exception.StorageException;
import lms.model.Loan;
import lms.storage.Repository;

public class SqliteLoanRepository implements Repository<Loan, String> {

    public SqliteLoanRepository(SqliteStorage storage) {
    }

    @Override
    public void save(Loan loan) {
        String sql = "INSERT OR REPLACE INTO loans(loan_id, copy_id, member_id, loan_date, due_date, return_date) " +
                "VALUES(?,?,?,?,?,?)";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, loan.getLoanId());
                statement.setString(2, loan.getCopyId());
                statement.setString(3, loan.getMemberId());
                statement.setString(4, toText(loan.getLoanDate()));
                statement.setString(5, toText(loan.getDueDate()));
                statement.setString(6, toText(loan.getReturnDate()));
                statement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to save loan " + loan.getLoanId(), ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    @Override
    public Optional<Loan> findById(String id) {
        String sql = "SELECT loan_id, copy_id, member_id, loan_date, due_date, return_date FROM loans WHERE loan_id = ?";
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
            throw new StorageException("Failed to load loan " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
        return Optional.empty();
    }

    @Override
    public List<Loan> findAll() {
        String sql = "SELECT loan_id, copy_id, member_id, loan_date, due_date, return_date FROM loans";
        List<Loan> loans = new ArrayList<>();
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to list loans", ex);
        } finally {
            storage.closeConnection(connection);
        }
        return loans;
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM loans WHERE loan_id = ?";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to delete loan " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM loans WHERE loan_id = ?";
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
            throw new StorageException("Failed to check loan " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    private Loan mapRow(ResultSet rs) throws SQLException {
        Loan loan = new Loan();
        loan.setLoanId(rs.getString("loan_id"));
        loan.setCopyId(rs.getString("copy_id"));
        loan.setMemberId(rs.getString("member_id"));
        loan.setLoanDate(parseDate(rs.getString("loan_date")));
        loan.setDueDate(parseDate(rs.getString("due_date")));
        loan.setReturnDate(parseDate(rs.getString("return_date")));
        return loan;
    }

    private String toText(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private LocalDate parseDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }
}