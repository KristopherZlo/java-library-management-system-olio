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
import lms.model.Member;
import lms.model.MemberType;
import lms.storage.Repository;

public class SqliteMemberRepository implements Repository<Member, String> {

    public SqliteMemberRepository(SqliteStorage storage) {
    }

    @Override
    public void save(Member member) {
        String sql = "INSERT OR REPLACE INTO members(member_id, name, email, type) VALUES(?,?,?,?)";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, member.getMemberId());
                statement.setString(2, member.getName());
                statement.setString(3, member.getEmail());
                statement.setString(4, member.getType().name());
                statement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to save member " + member.getMemberId(), ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    @Override
    public Optional<Member> findById(String id) {
        String sql = "SELECT member_id, name, email, type FROM members WHERE member_id = ?";
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
            throw new StorageException("Failed to load member " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
        return Optional.empty();
    }

    @Override
    public List<Member> findAll() {
        String sql = "SELECT member_id, name, email, type FROM members";
        List<Member> members = new ArrayList<>();
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to list members", ex);
        } finally {
            storage.closeConnection(connection);
        }
        return members;
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM members WHERE member_id = ?";
        Connection connection = null;
        try {
            connection = storage.connect();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new StorageException("Failed to delete member " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    @Override
    public boolean existsById(String id) {
        String sql = "SELECT 1 FROM members WHERE member_id = ?";
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
            throw new StorageException("Failed to check member " + id, ex);
        } finally {
            storage.closeConnection(connection);
        }
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        Member member = new Member();
        member.setMemberId(rs.getString("member_id"));
        member.setName(rs.getString("name"));
        member.setEmail(rs.getString("email"));
        member.setType(MemberType.valueOf(rs.getString("type")));
        return member;
    }
}