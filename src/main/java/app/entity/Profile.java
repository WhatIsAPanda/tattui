package app.entity;

import javafx.scene.SubScene;
import javafx.scene.image.Image;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Profile {
    public int id;
    public String username;
    public String password;
    public List<Post> posts;
    public String biography;
    public String profile_picture_url;
    public Image profile_picture_image;
//    public String address;
//    public double longtitude;
//    public double latitude;

    public Profile(int id, String username, String password, String profile_picture_url, List<Post> posts, String biography) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.profile_picture_url = profile_picture_url;
        this.posts = posts;
        this.biography = biography;
        this.profile_picture_image = new Image(profile_picture_url);
//        this.address = address;
//        this.longtitude = longtitude;
//        this.latitude = latitude;
    }
}
