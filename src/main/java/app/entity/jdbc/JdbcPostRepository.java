package app.entity.jdbc;

import app.db.DbConnectionProvider;
import app.entity.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class JdbcPostRepository implements PostRepository {

    @Override
    public List<PostWithAuthor> findLatest(int limit, int offset) throws SQLException {
        String sql = """
            SELECT p.id, p.caption, p.postURL, p.account_id,
                   a.account_id AS acc_id, a.username, a.profile_picture_url,
                   ar.work_address, ar.work_longitude, ar.work_latitude, a.biography
            FROM Posts2 p
            JOIN Accounts a ON a.account_id = p.account_id
            LEFT JOIN Artists ar ON ar.account_id = a.account_id
            ORDER BY p.id DESC
            LIMIT ? OFFSET ?
        """;
        try (Connection c = DbConnectionProvider.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                return map(rs);
            }
        }
    }

    @Override
    public List<PostWithAuthor> search(String q, int limit, int offset) throws SQLException {
        String sql = """
            SELECT p.id, p.caption, p.postURL, p.account_id,
                   a.account_id AS acc_id, a.username, a.profile_picture_url,
                   ar.work_address, ar.work_longitude, ar.work_latitude, a.biography
            FROM Posts2 p
            JOIN Accounts a ON a.account_id = p.account_id
            LEFT JOIN Artists ar ON ar.account_id = a.account_id
            WHERE LOWER(p.caption) LIKE LOWER(?) OR LOWER(a.username) LIKE LOWER(?)
            ORDER BY p.id DESC
            LIMIT ? OFFSET ?
        """;
        try (Connection c = DbConnectionProvider.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + q + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                return map(rs);
            }
        }
    }

    private static List<PostWithAuthor> map(ResultSet rs) throws SQLException {
        List<PostWithAuthor> out = new ArrayList<>();
        while (rs.next()) {
            Post post = new Post(
                    rs.getInt("id"),
                    rs.getString("caption"),
                    rs.getString("postURL")
            );
            Profile author = new Profile(
                    rs.getInt("acc_id"),
                    rs.getString("username"),
                    rs.getString("profile_picture_url"),
                    rs.getString("biography"),
                    rs.getString("work_address"),
                    rs.getDouble("work_longitude"),
                    rs.getDouble("work_latitude"),
                    java.util.List.of()
            );
            out.add(new PostWithAuthor(post, author));
        }
        return out;
    }
}
