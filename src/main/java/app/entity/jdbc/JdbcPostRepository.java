package app.entity.jdbc;

import app.entity.DatabaseConnector;
import app.entity.PostRepository;
import app.entity.PostWithAuthor;

import java.sql.SQLException;
import java.util.List;

public final class JdbcPostRepository implements PostRepository {

    @Override
    public List<PostWithAuthor> findLatest(int limit, int offset) throws SQLException {
        return DatabaseConnector.fetchPostsWithAuthors(limit, offset);
    }

    @Override
    public List<PostWithAuthor> search(String q, int limit, int offset) throws SQLException {
        return DatabaseConnector.searchPostsWithAuthors(q, limit, offset);
    }
}
