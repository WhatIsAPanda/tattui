package app.entity;

import java.util.List;

public class Profile {
    public int id;
    public String username;
    public String password;
    public List<Post> posts;
    public String biography;
    public String profile_picture;
//    public String address;
//    public double longtitude;
//    public double latitude;

    public Profile(int id, String username, String password, String profile_picture, List<Post> posts, String biography) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.profile_picture = profile_picture;
        this.posts = posts;
        this.biography = biography;
//        this.address = address;
//        this.longtitude = longtitude;
//        this.latitude = latitude;
    }
}
