package app.entity;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseConnector {
    private static Connection conn;
    private static final String url = System.getenv("DATABASE_URL");
    private static final String user = System.getenv("DATABASE_USER");
    private static final String password = System.getenv("DATABASE_PASSWORD");
    static{
        try{
            conn = DriverManager.getConnection(url,user,password);
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
            profiles.add(new Profile(user_id,username,password,profile_picture,posts,biography));
        }
        return profiles;
    }

    public static List<Profile> getProfilesWithinBounds() {
        return Collections.emptyList();
    }

    public static List<Profile> getProfilesLike() {
        return Collections.emptyList();
    }





}
