package app.entity.jdbc;

import app.entity.DatabaseConnector;
import app.entity.DesignRepository;
import app.entity.DesignWithAuthor;

import java.sql.SQLException;
import java.util.List;

public final class JdbcDesignRepository implements DesignRepository {

    @Override
    public List<DesignWithAuthor> findLatest(int limit, int offset) throws SQLException {
        return DatabaseConnector.fetchDesignsWithAuthors(limit, offset);
    }

    @Override
    public List<DesignWithAuthor> search(String query, int limit, int offset) throws SQLException {
        return DatabaseConnector.searchDesignsWithAuthors(query, limit, offset);
    }
}
