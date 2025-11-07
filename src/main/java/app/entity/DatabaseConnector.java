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
                "SELECT * FROM PostOwnerships as PO " +
                        "LEFT JOIN Users as U ON PO.user_id = U.id " +
                        "LEFT JOIN Posts as P ON PO.post_id = P.id WHERE U.username = ?");
        profileQueryStatement.setString(1, queryUsername);
        ResultSet rs = profileQueryStatement.executeQuery();
        if(!rs.next()) {
            return null;
        }
        int user_id = rs.getInt("user_id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String profile_picture = rs.getString("profile_picture");
        String biography = rs.getString("biography");
        List<Post> posts = new ArrayList<>();
        int post_id = rs.getInt("post_id");
        String postURL = rs.getString("postURL");
        String caption = rs.getString("caption");

        Post newPost = new Post(post_id,caption,postURL);
        posts.add(newPost);

        while(rs.next()) {
            post_id = rs.getInt("post_id");
            postURL = rs.getString("postURL");
            caption = rs.getString("caption");
            newPost =  new Post(post_id,caption,postURL);
            posts.add(newPost);
        }
        return new Profile(user_id, username,password,profile_picture,posts,biography);
    }

    public static List<Profile> getProfileWithinBounds() {
        return Collections.emptyList();
    }

    public static List<Profile> getProfilesLike() {
        return Collections.emptyList();
    }





}
