package app.entity;

import java.sql.SQLException;
import java.util.List;

public interface PostRepository {
    List<PostWithAuthor> findLatest(int limit, int offset) throws SQLException;
    List<PostWithAuthor> search(String query, int limit, int offset) throws SQLException;
}
