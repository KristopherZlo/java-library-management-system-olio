package lms.storage.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lms.exception.StorageException;
import lms.model.Reservation;
import lms.model.ReservationStatus;
import lms.storage.Repository;

public class SqliteReservationRepository implements Repository<Reservation, String> {
    private final SqliteStorage storage;

    public SqliteReservationRepository(SqliteStorage storage) {
        this.storage = storage;
    }

    @Override
    public void save(Reservation reservation) {
        String sql = "INSERT OR REPLACE INTO reservations(res_id, isbn, member_id, created_at, status) VALUES(?,?,?,?,?)";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, reservation.getReservationId());
                statement.setString(2, reservation.getIsbn());
                statement.setString(3, reservation.getMemberId());
                statement.setString(4, toText(reservation.getCreatedAt()));
                statement.setString(5, reservation.getStatus().name());
                statement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to save reservation " + reservation.getReservationId(), ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    @Override
    public Optional<Reservation> findById(String id) {
        String sql = "SELECT res_id, isbn, member_id, created_at, status FROM reservations WHERE res_id = ?";
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
            throw new StorageException("Failed to load reservation " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
        return Optional.empty();
    }

    @Override
    public List<Reservation> findAll() {
        String sql = "SELECT res_id, isbn, member_id, created_at, status FROM reservations";
        List<Reservation> reservations = new ArrayList<>();
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to list reservations", ex);
        } finally {
            storage.closeConnection(connection);
        }
        return reservations;
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM reservations WHERE res_id = ?";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to delete reservation " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM reservations WHERE res_id = ?";
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
            throw new StorageException("Failed to check reservation " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setReservationId(rs.getString("res_id"));
        reservation.setIsbn(rs.getString("isbn"));
        reservation.setMemberId(rs.getString("member_id"));
        reservation.setCreatedAt(parseDate(rs.getString("created_at")));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        return reservation;
    }

    private String toText(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private LocalDate parseDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }
}