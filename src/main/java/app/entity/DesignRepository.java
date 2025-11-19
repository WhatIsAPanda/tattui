package app.entity;

import java.sql.SQLException;
import java.util.List;

public interface DesignRepository {
    List<DesignWithAuthor> findLatest(int limit, int offset) throws SQLException;
    List<DesignWithAuthor> search(String query, int limit, int offset) throws SQLException;
}
