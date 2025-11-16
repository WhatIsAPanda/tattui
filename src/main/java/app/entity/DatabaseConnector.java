package app.entity;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class DatabaseConnector {
    private static Connection conn;
    private static final String URL = System.getenv("DATABASE_URL");
    private static final String USER = System.getenv("DATABASE_USER");
    private static final String PASSWORD = System.getenv("DATABASE_PASSWORD");
    private static final String ACCOUNT_ID_STRING = System.getenv("account_id");
    private DatabaseConnector() {}

    static {
        conn = openConnection();
    }

    public static synchronized boolean ensureConnection() {
        if (isConnected()) {
            return true;
        }
        closeQuietly();
        conn = openConnection();
        return conn != null;
    }

    private static boolean isConnected() {
        if (conn == null) {
            return false;
        }
        try {
            return !conn.isClosed();
        } catch (SQLException _) {
            return false;
        }
    }

    private static Connection openConnection() {
        try {
            return createConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void closeQuietly() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException _) {
                // Ignore
            } finally {
                conn = null;
            }
        }
    }

    private static Connection createConnection() throws SQLException {
        String dbUrl = URL;
        String dbUser = USER;
        String dbPassword = PASSWORD;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("C:\\SchoolProjects\\keys.txt")) {
                props.load(fis);
                dbUrl = props.getProperty("url");
                dbUser = props.getProperty("user");
                dbPassword = props.getProperty("password");
            } catch (IOException e) {
                throw new SQLException("Failed to load database credentials", e);
            }
        }
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new SQLException("Database credentials are not configured.");
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }


    public static Account getAccountByUsername(String queryUsername) throws SQLException {
        PreparedStatement stmt = DatabaseConnector.conn.prepareStatement("""
                SELECT *
                FROM Accounts AS A
                LEFT JOIN AccountStylePreferences AS ASP ON ASP.account_id = A.account_id
                WHERE A.username = ?;
                """);
        stmt.setString(1, queryUsername);
        ResultSet rs = stmt.executeQuery();
        return convertToAccount(rs);
    }
    private static Account convertToAccount(ResultSet rs) throws SQLException {
        if(!rs.next()){
            return null;
        }
        List<String> stylesList = new ArrayList<>();
        int id = rs.getInt(ACCOUNT_ID_STRING);
        String username = rs.getString("username");
        String password = rs.getString("password");
        String profilePictureUrl = rs.getString("profile_picture_url");
        double homeLatitude = rs.getDouble("home_latitude");
        double homeLongitude = rs.getDouble("home_longitude");
        String style = rs.getString("style_name");
        do {
            stylesList.add(style);
        } while (rs.next());
        return new Account(id,username,password,profilePictureUrl,homeLatitude,homeLongitude,stylesList);
    }

    public static Profile getProfileByUsername(String queryUsername) throws SQLException {
        PreparedStatement profileQueryStatement = DatabaseConnector.conn.prepareStatement("""
                SELECT *
                FROM Artists AS A
                LEFT JOIN Posts2 AS P ON P.account_id = A.account_id
                LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.artist_id = A.artist_id
                LEFT JOIN Accounts AS Acc ON Acc.account_id = A.account_id
                WHERE username = ? ;
                """);
        profileQueryStatement.setString(1, queryUsername);
        ResultSet rs = profileQueryStatement.executeQuery();
        List<Profile> artistProfilesList = convertToArtistProfiles(rs);
        return artistProfilesList.isEmpty() ? null : artistProfilesList.getFirst();
    }
    private static List<Profile> convertToArtistProfiles(ResultSet rs) throws SQLException {
        if(!rs.next()){
            return Collections.emptyList();
        }
        List<Profile> artistProfiles = new ArrayList<>();
        while(!rs.isAfterLast()) {
            int accountId = rs.getInt(ACCOUNT_ID_STRING);
            String username = rs.getString("username");
            String profilePictureUrl = rs.getString("profile_picture_url");
            String biography = rs.getString("biography");
            String workAddress = rs.getString("work_address");
            double workLongitude = rs.getDouble("work_longitude");
            double workLatitude = rs.getDouble("work_latitude");

            List<String> styles = new ArrayList<>();
            String style = rs.getString("style_name");
            if(style != null) {
                styles.add(style);
            }
            while(rs.next()) {
                if(rs.getInt(ACCOUNT_ID_STRING) != accountId) {
                    break;
                }
                styles.add(style);
            }
            artistProfiles.add(new Profile(accountId, username, profilePictureUrl,biography, workAddress, workLongitude, workLatitude, styles));
        }
        return artistProfiles;

    }

    public static List<Profile> getProfilesLike(String pattern) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("""
                SELECT *
                FROM Artists AS A
                LEFT JOIN Posts2 AS P ON P.account_id = A.account_id
                LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.artist_id = A.artist_id
                LEFT JOIN Accounts AS ACC ON ACC.account_id = A.account_id
                WHERE username LIKE ?
                """);
        stmt.setString(1, "%" + pattern + "%");
        ResultSet rs = stmt.executeQuery();
        return convertToArtistProfiles(rs);
    }

    public static List<Profile> getProfilesWithinBounds(double latitudeFrom, double latitudeTo, double longitudeFrom, double longitudeTo) throws SQLException {
        PreparedStatement profileQueryStatement = DatabaseConnector.conn.prepareStatement("""
                SELECT *
                FROM Artists AS A
                LEFT JOIN Posts2 AS P ON P.account_id = A.account_id
                LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.artist_id = A.artist_id
                LEFT JOIN Accounts AS ACC ON ACC.account_id = A.account_id
                WHERE A.work_latitude >= ? AND A.work_latitude <= ? AND A.work_longitude >= ? AND A.work_longitude <= ?;
                """);
        profileQueryStatement.setDouble(1, latitudeFrom);
        profileQueryStatement.setDouble(2, latitudeTo);
        profileQueryStatement.setDouble(3, longitudeFrom);
        profileQueryStatement.setDouble(4, longitudeTo);
        ResultSet rs = profileQueryStatement.executeQuery();
        List<Profile> profileList = convertToArtistProfiles(rs);
        if(profileList.isEmpty()){
            return Collections.emptyList();
        }
        else {
            return profileList;
        }
    }

    public static void createUser(String username, String password, boolean isArtist) throws SQLException {
        PreparedStatement insertAccountsStmt = DatabaseConnector.conn.prepareStatement("""
                INSERT INTO Accounts(username, password)
                VALUES (?, ?);
                """, Statement.RETURN_GENERATED_KEYS
        );
        insertAccountsStmt.setString(1, username);
        insertAccountsStmt.setString(2, password);
        insertAccountsStmt.executeUpdate();


        ResultSet rs = insertAccountsStmt.getGeneratedKeys();
        if(rs.next()) {
            int userId = rs.getInt(1);
            if(isArtist) {
                PreparedStatement insertArtistsStmt = DatabaseConnector.conn.prepareStatement("""
                        INSERT INTO Artists(account_id)
                        VALUES (?);
                        """);
                insertArtistsStmt.setInt(1, userId);
                insertArtistsStmt.executeUpdate();
            }
        }
    }







}
