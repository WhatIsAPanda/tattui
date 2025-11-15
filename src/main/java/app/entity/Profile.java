package app.entity;

import javafx.scene.SubScene;
import javafx.scene.image.Image;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Profile {
    private int id;
    private String username;
    private String password;
    private List<Post> posts;
    private String biography;
    private String profile_picture_url;
    private Image profile_picture_image;
    private String address;
    private double longitude;
    private double latitude;
    private String tags;

    public Profile(int id, String username, String password, String profile_picture_url, List<Post> posts, String biography, String address, double longtitude, double latitude, String tags) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.profile_picture_url = profile_picture_url;
        this.posts = posts;
        this.biography = biography;
        this.profile_picture_image = new Image(profile_picture_url);
        this.address = address;
        this.longitude = longtitude;
        this.latitude = latitude;
        this.tags = tags;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public String getBiography() {
        return biography;
    }

    public String getProfile_picture_url() {
        return profile_picture_url;
    }

    public Image getProfile_picture_image() {
        return profile_picture_image;
    }

    public String getAddress() {
        return address;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public String getTags() {
        return tags;
    }
}
