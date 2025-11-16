package app.entity;

import javafx.scene.image.Image;

import java.util.List;

public class Account {
    private int account_id;
    private String username;
    private String password;
    private String profile_picture_url;
    private Image profile_picture;
    private double home_latitude;
    private double home_longitude;
    private List<String> style_preferences;

    public Account(int account_id, String username, String password, String profile_picture_url, double home_latitude, double home_longitude, List<String> style_preferences) {
        this.account_id = account_id;
        this.username = username;
        this.password = password;
        this.profile_picture_url = profile_picture_url;
        this.home_latitude = home_latitude;
        this.home_longitude = home_longitude;
        this.style_preferences = style_preferences;
        this.profile_picture = new Image(profile_picture_url);
    }

    public int getAccount_id() {
        return account_id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getProfile_picture_url() {
        return profile_picture_url;
    }

    public Image getProfile_picture() {
        return profile_picture;
    }

    public double getHome_latitude() {
        return home_latitude;
    }

    public double getHome_longitude() {
        return home_longitude;
    }

    public List<String> getStyle_preferences() {
        return style_preferences;
    }
}
