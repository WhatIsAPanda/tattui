package app.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DbConnectionProvider {
    private DbConnectionProvider() {}

    public static Connection open() throws SQLException {
        String url  = System.getenv("DATABASE_URL");
        String user = System.getenv("DATABASE_USER");
        String pass = System.getenv("DATABASE_PASSWORD");

        if (url == null || user == null || pass == null) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("C:\\SchoolProjects\\keys.txt")) {
                props.load(fis);
            } catch (IOException e) {
                throw new SQLException("DB credentials not configured (env vars or keys.txt).", e);
            }
            url  = props.getProperty("url");
            user = props.getProperty("user");
            pass = props.getProperty("password");
        }
        return DriverManager.getConnection(url, user, pass);
    }
}
