package app.entity;

import javafx.scene.image.Image;

import java.util.List;

public class Profile {
    private int account_id;
    private String username;
    private String biography;
    private String profile_picture_url;
    private Image profile_picture;
    private String work_address;
    private double work_longitude;
    private double work_latitude;
    private List<String> styles_list;
    private List<Post> artistPosts;

    public Profile(int account_id,  String username, String profile_picture_url, String biography, String work_address, double work_longitude, double work_latitude, List<String> styles_list) {
        this.account_id = account_id;
        this.username = username;
        this.biography = biography;
        this.work_address = work_address;
        this.work_longitude = work_longitude;
        this.work_latitude = work_latitude;
        this.styles_list = styles_list;
        this.profile_picture_url = profile_picture_url;
        this.profile_picture = new Image(profile_picture_url);
    }

    public int getAccountId() {
        return account_id;
    }

    public String getUsername() {
        return username;
    }

    public String getBiography() {
        return biography;
    }

    public String getProfilePictureURL() {
        return profile_picture_url;
    }

    public String getAddress() {
        return work_address;
    }

    public double getLongitude() {
        return work_longitude;
    }

    public double getLatitude() {
        return work_latitude;
    }

    public List<String> getStylesList() {
        return styles_list;
    }
    public Image getProfilePicture() {
        return profile_picture;
    }
    public void setArtistPosts(List<Post> artistPosts) {
        this.artistPosts = artistPosts;
    }
    public List<Post> getArtistPosts() {
        return artistPosts;
    }

}
