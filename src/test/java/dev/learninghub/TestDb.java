package dev.learninghub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

/** Direct JDBC access for test setup and checks that must bypass the API (e.g. another owner's data). */
public final class TestDb {

    private TestDb() {
    }

    public static long insertNote(DataSource dataSource, String ownerId, String title) {
        String sql = """
                INSERT INTO note (id, owner_id, title, body, course_week, created_at, updated_at)
                VALUES (nextval('note_seq'), ?, ?, 'body', NULL, now(), now())
                RETURNING id""";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.fromString(ownerId));
            statement.setString(2, title);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static long insertNote(DataSource dataSource, String ownerId, String title, Instant updatedAt) {
        String sql = """
                INSERT INTO note (id, owner_id, title, body, course_week, created_at, updated_at)
                VALUES (nextval('note_seq'), ?, ?, 'body', NULL, ?, ?)
                RETURNING id""";
        OffsetDateTime at = updatedAt.atOffset(ZoneOffset.UTC);
        return queryLong(dataSource, sql, UUID.fromString(ownerId), title, at, at);
    }

    public static long insertTag(DataSource dataSource, String ownerId, String name) {
        String sql = "INSERT INTO tag (id, owner_id, name) VALUES (nextval('tag_seq'), ?, ?) RETURNING id";
        return queryLong(dataSource, sql, UUID.fromString(ownerId), name);
    }

    public static void linkTag(DataSource dataSource, long noteId, long tagId) {
        try {
            execute(dataSource, "INSERT INTO note_tag (note_id, tag_id) VALUES (?, ?)", noteId, tagId);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static long countTags(DataSource dataSource, String ownerId, String name) {
        return queryLong(dataSource, "SELECT count(*) FROM tag WHERE owner_id = ? AND name = ?",
                UUID.fromString(ownerId), name);
    }

    public static long countTagLinks(DataSource dataSource, long tagId) {
        return queryLong(dataSource, "SELECT count(*) FROM note_tag WHERE tag_id = ?", tagId);
    }

    public static long queryLong(DataSource dataSource, String sql, Object... params) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Optional<String> noteOwner(DataSource dataSource, long noteId) {
        return queryString(dataSource, "SELECT owner_id::text FROM note WHERE id = ?", noteId);
    }

    public static Optional<String> noteTitle(DataSource dataSource, long noteId) {
        return queryString(dataSource, "SELECT title FROM note WHERE id = ?", noteId);
    }

    public static Optional<String> queryString(DataSource dataSource, String sql, Object... params) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void execute(DataSource dataSource, String sql, Object... params) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            statement.execute();
        }
    }
}
