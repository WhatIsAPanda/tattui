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

    public static Profile getProfileByUsername(String queryUsername) throws SQLException {
        PreparedStatement profileQueryStatement = DatabaseConnector.conn.prepareStatement(
                """
                SELECT *
                FROM Users AS U
                LEFT JOIN PostOwnerships AS PO ON PO.user_id = U.id
                LEFT JOIN Posts AS P ON PO.postId = P.id
                WHERE U.username = ?;
                """);
        profileQueryStatement.setString(1, queryUsername);
        ResultSet rs = profileQueryStatement.executeQuery();
        List<Profile> profileList = convertToProfileList(rs);
        if(profileList.isEmpty()){
            return null;
        }
        else {
            return profileList.getFirst();
        }
    }

    public static List<Profile> getProfilesLike(String pattern) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                """
                SELECT *
                FROM Users AS U
                LEFT JOIN PostOwnerships AS PO ON PO.user_id = U.id
                LEFT JOIN Posts AS P ON PO.postId = P.id
                WHERE U.username LIKE ?;
                """);
        stmt.setString(1, "%" + pattern + "%");
        ResultSet rs = stmt.executeQuery();
        return convertToProfileList(rs);
    }

    public static List<Profile> getProfilesWithinBounds(double latitudeFrom, double latitudeTo, double longitudeFrom, double longitudeTo) throws SQLException {
        PreparedStatement profileQueryStatement = DatabaseConnector.conn.prepareStatement(
                """
                SELECT *
                FROM Users AS U
                LEFT JOIN PostOwnerships AS PO ON PO.user_id = U.id
                LEFT JOIN Posts AS P ON PO.postId = P.id
                WHERE U.latitude >= ? AND U.latitude <= ? AND U.longitude >= ? AND U.longitude <= ?;
                """);
        profileQueryStatement.setDouble(1, latitudeFrom);
        profileQueryStatement.setDouble(2, latitudeTo);
        profileQueryStatement.setDouble(3, longitudeFrom);
        profileQueryStatement.setDouble(4, longitudeTo);
        ResultSet rs = profileQueryStatement.executeQuery();
        List<Profile> profileList = convertToProfileList(rs);
        if(profileList.isEmpty()){
            return Collections.emptyList();
        }
        else {
            return profileList;
        }
    }

    public static List<Profile> getProfilesLike() {
        return Collections.emptyList();
    }
    private static List<Profile> convertToProfileList(ResultSet rs) throws SQLException {
        if(rs.isAfterLast()) {
            return Collections.emptyList();
        }
        List<Profile> profiles = new ArrayList<>();
        rs.next();
        while(!rs.isAfterLast()) {
            int userId = rs.getInt("user_id");
            String username = rs.getString("username");
            String password = rs.getString("password");
            String profilePicture = rs.getString("profile_picture");
            String biography = rs.getString("biography");
            double longitude = rs.getDouble("longitude");
            double latitude = rs.getDouble("latitude");

            List<Post> posts = new ArrayList<>();
            int postId = rs.getInt("post_id");
            if(postId != 0) {
                String postURL = rs.getString("postURL");
                String caption = rs.getString("caption");
                Post newPost = new Post(postId,caption,postURL);
                posts.add(newPost);
            }
            while(rs.next()) {
                if(rs.getInt("user_id") != userId) {
                    break;
                }
                postId = rs.getInt("post_id");
                String postURL = rs.getString("postURL");
                String caption = rs.getString("caption");
                Post newPost =  new Post(postId,caption,postURL);
                posts.add(newPost);
            }
            profiles.add(new Profile(userId,username,password,profilePicture,posts,biography, "FIller", longitude, latitude, "Balls"));
        }
        return profiles;
    }







}
