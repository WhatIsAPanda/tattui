package app.entity.jdbc;

import app.db.DbConnectionProvider;
import app.entity.Post;
import app.entity.Profile;
import app.entity.PostRepository;
import app.entity.PostWithAuthor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcPostRepository implements PostRepository {

    @Override
    public List<PostWithAuthor> findLatest(int limit, int offset) throws SQLException {
        String sql = """
                    SELECT
                        p.id              AS id,
                        p.caption         AS caption,
                        p.postURL         AS postURL,
                        u.id              AS acc_id,
                        u.username        AS username,
                        u.profile_picture AS profile_picture_url,
                        u.biography       AS biography,
                        u.address         AS work_address,
                        u.longitude       AS work_longitude,
                        u.latitude        AS work_latitude
                    FROM Posts p
                    LEFT JOIN PostOwnerships po ON po.post_id = p.id
                    LEFT JOIN Users u           ON u.id       = po.user_id
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
                    SELECT
                        p.id              AS id,
                        p.caption         AS caption,
                        p.postURL         AS postURL,
                        u.id              AS acc_id,
                        u.username        AS username,
                        u.profile_picture AS profile_picture_url,
                        u.biography       AS biography,
                        u.address         AS work_address,
                        u.longitude       AS work_longitude,
                        u.latitude        AS work_latitude
                    FROM Posts p
                    LEFT JOIN PostOwnerships po ON po.post_id = p.id
                    LEFT JOIN Users u           ON u.id       = po.user_id
                    WHERE LOWER(p.caption) LIKE LOWER(?) OR LOWER(u.username) LIKE LOWER(?)
                    ORDER BY p.id DESC
                    LIMIT ? OFFSET ?
                """;

        try (Connection c = DbConnectionProvider.open();
                PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + (q == null ? "" : q) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                return map(rs);
            }
        }
    }

    /** Maps the aliased columns above into domain objects. */
    private static List<PostWithAuthor> map(ResultSet rs) throws SQLException {
        List<PostWithAuthor> out = new ArrayList<>();
        while (rs.next()) {
            Post post = new Post(
                    rs.getInt("id"),
                    rs.getString("caption"),
                    rs.getString("postURL"));

            Profile author = new Profile(
                    rs.getInt("acc_id"),
                    rs.getString("username"),
                    rs.getString("profile_picture_url"),
                    rs.getString("biography"),
                    rs.getString("work_address"),
                    safeDouble(rs, "work_longitude"),
                    safeDouble(rs, "work_latitude"),
                    java.util.List.of());

            out.add(new PostWithAuthor(post, author));
        }
        return out;
    }

    private static double safeDouble(ResultSet rs, String col) {
        try {
            double v = rs.getDouble(col);
            if (rs.wasNull())
                return 0.0;
            return v;
        } catch (SQLException e) {
            return 0.0;
        }
    }
}
