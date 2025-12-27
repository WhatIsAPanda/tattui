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
    private static final String URL = "jdbc:sqlite:tattui.db";
    private static final String ACCOUNT_ID_STRING = "account_id";

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
        return DriverManager.getConnection(URL);
    }

    public static Profile getFullProfile(Profile profileSkeleton) throws SQLException {
        PreparedStatement preparedStatement = DatabaseConnector.conn.prepareStatement(
                "SELECT * \n" +
                    "FROM Artists AS A\n" +
                    "LEFT JOIN Posts AS P ON P.account_id = A.account_id\n" +
                    "LEFT JOIN Accounts AS ACC ON ACC.account_id = A.account_id\n" +
                        "WHERE ACC.username = ?;"
        );
        preparedStatement.setString(1, profileSkeleton.getUsername());
        ResultSet resultSet = preparedStatement.executeQuery();
        List<Post> artistPosts = getArtistPosts(resultSet);
        profileSkeleton.setArtistPosts(artistPosts);
        return profileSkeleton;

    }

    public static List<Post> getArtistPosts(ResultSet rs) throws SQLException {
        if(!rs.next()) {
            return null;
        }
        List<Post> posts = new ArrayList<>();
        do {
            int postId = rs.getInt("post_id");
            String caption = rs.getString("caption");
            int postOwner = rs.getInt("account_id");
            String postPictureURL = rs.getString("post_picture_url");
            String keyWords = rs.getString("keywords");
            String ownerUsername = rs.getString("username");
            posts.add(new Post(postId,caption,postPictureURL,postOwner,keyWords,ownerUsername));
        }
        while(rs.next());

        return posts;
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
                LEFT JOIN Posts AS P ON P.account_id = A.account_id
                LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.account_id = A.artist_id
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
    private static List<Post> convertToPosts(ResultSet rs) throws SQLException {
        List<Post> posts = new ArrayList<>();
        while(rs.next()) {

        }
        return Collections.EMPTY_LIST;

    }

    public static List<Profile> getProfilesLike(String pattern) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("""
                SELECT *
                FROM Artists AS A
                LEFT JOIN Posts AS P ON P.account_id = A.account_id
                LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.account_id = A.artist_id
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
                LEFT JOIN Posts AS P ON P.account_id = A.account_id
                LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.account_id = A.artist_id
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
    public static void getLatestPosts() throws SQLException {
        Statement getLatestPostsStmt = DatabaseConnector.conn.createStatement();
        String sql = "SELECT * FROM Posts ORDER BY id DESC";
        ResultSet rs = getLatestPostsStmt.executeQuery(sql);


    }
}
