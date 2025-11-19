package app.entity;

import app.util.ImageResolver;
import javafx.scene.image.Image;

import java.util.List;

public class Account {
    private int accountId;
    private String username;
    private String password;
    private String profilePictureUrl;
    private Image profilePicture;
    private double homeLatitude;
    private double homeLongitude;
    private List<String> stylePreferences;

    public Account(int accountId, String username, String password, String profilePictureUrl, double homeLatitude,
            double homeLongitude, List<String> stylePreferences) {
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.profilePictureUrl = profilePictureUrl;
        this.homeLatitude = homeLatitude;
        this.homeLongitude = homeLongitude;
        this.stylePreferences = stylePreferences;
        try {
            this.profilePicture = ImageResolver.loadAny(
                    profilePictureUrl,
                    "/db/db_resources/" + profilePictureUrl);
        } catch (IllegalArgumentException _) {
            this.profilePicture = null;
        }
    }

    public int getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getprofilePictureUrl() {
        return profilePictureUrl;
    }

    public Image getprofilePicture() {
        return profilePicture;
    }

    public double gethomeLatitude() {
        return homeLatitude;
    }

    public double gethomeLongitude() {
        return homeLongitude;
    }

    public List<String> getstylePreferences() {
        return stylePreferences;
    }
}
