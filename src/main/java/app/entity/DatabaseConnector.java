package app.entity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    static{
        try{
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
                    e.printStackTrace();
                    System.out.println(dbUser);
                }
            } 
            conn = DriverManager.getConnection(dbUrl,dbUser,dbPassword);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Profile getProfileByUsername(String queryUsername) throws SQLException {
        PreparedStatement profileQueryStatement = DatabaseConnector.conn.prepareStatement(
                "SELECT * FROM Users as U \n" +
                        "LEFT JOIN PostOwnerships as PO ON PO.user_id = U.id\n" +
                        "LEFT JOIN Posts as P ON PO.post_id = P.id\n" +
                        "WHERE U.username = ?;");
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
            "SELECT * FROM Users AS U \n" +
            "LEFT JOIN PostOwnerships AS PO ON PO.user_id = U.id\n" +
            "LEFT JOIN Posts AS P ON PO.post_id = P.id\n" +
            "WHERE U.username ILIKE ?;"
        );
        stmt.setString(1, "%" + pattern + "%");
        ResultSet rs = stmt.executeQuery();
        return convertToProfileList(rs);
    }

    public static List<Profile> getProfilesWithinBounds(double latitudeFrom, double latitudeTo, double longitudeFrom, double longitudeTo) throws SQLException {
        PreparedStatement profileQueryStatement = DatabaseConnector.conn.prepareStatement(
                "SELECT * FROM Users as U \n" +
                        "LEFT JOIN PostOwnerships as PO ON PO.user_id = U.id\n" +
                        "LEFT JOIN Posts as P ON PO.post_id = P.id\n" +
                        "WHERE U.latitude >= ? AND U.latitude <= ? AND U.longitude >= ? AND U.longitude <= ?");
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
            int user_id = rs.getInt("user_id");
            String username = rs.getString("username");
            String password = rs.getString("password");
            String profile_picture = rs.getString("profile_picture");
            String biography = rs.getString("biography");
            // String address = rs.getString("address");
            double longitude = rs.getDouble("longitude");
            double latitude = rs.getDouble("latitude");

            List<Post> posts = new ArrayList<>();
            int post_id = rs.getInt("post_id");
            if(post_id != 0) {
                String postURL = rs.getString("postURL");
                String caption = rs.getString("caption");
                Post newPost = new Post(post_id,caption,postURL);
                posts.add(newPost);
            }
            while(rs.next()) {
                if(!(rs.getInt("user_id") == user_id)) {
                    break;
                }
                post_id = rs.getInt("post_id");
                String postURL = rs.getString("postURL");
                String caption = rs.getString("caption");
                Post newPost =  new Post(post_id,caption,postURL);
                posts.add(newPost);
            }
            profiles.add(new Profile(user_id,username,password,profile_picture,posts,biography, "FIller", longitude, latitude, "Balls"));
        }
        return profiles;
    }







}
