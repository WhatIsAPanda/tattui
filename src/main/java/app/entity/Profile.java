package app.entity;

import app.util.ImageResolver;
import javafx.scene.image.Image;

import java.util.Collections;
import java.util.List;

public class Profile {
    private static final boolean headless = Boolean.getBoolean("HEADLESS_TESTS");

    private int accountId;
    private String username;
    private String biography;
    private String profilePictureUrl;
    private Image profilePicture;
    private String workAddress;
    private double workLongitude;
    private double workLatitude;
    private List<String> stylesList;
    private List<Post> artistPosts;

    public Profile(int accountId, String username, String profilePictureUrl, String biography,
            List<String> stylesList, WorkLocation workLocation) {
        this.accountId = accountId;
        this.username = username;
        this.biography = biography;
        if (workLocation != null) {
            this.workAddress = workLocation.address;
            this.workLongitude = workLocation.longitude;
            this.workLatitude = workLocation.latitude;
        }
        this.stylesList = stylesList == null ? Collections.emptyList() : List.copyOf(stylesList);
        this.profilePictureUrl = profilePictureUrl;
        if (!headless) {
            try {
                this.profilePicture = ImageResolver.loadAny(
                        profilePictureUrl,
                        "/db/db_resources/" + profilePictureUrl);
            } catch (IllegalArgumentException _) {
                this.profilePicture = null;
            }
        }
    }

    public int getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getProfilePictureURL() {
        return profilePictureUrl;
    }

    public Image getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePictureUrl, Image image) {
        this.profilePictureUrl = profilePictureUrl;
        this.profilePicture = image;
    }

    public double getWorkLongitude() {
        return workLongitude;
    }

    public void setWorkLongitude(double workLongitude) {
        this.workLongitude = workLongitude;
    }

    public double getWorkLatitude() {
        return workLatitude;
    }

    public void setWorkLatitude(double workLatitude) {
        this.workLatitude = workLatitude;
    }

    public String getAddress() {
        return workAddress;
    }

    public List<String> getStylesList() {
        return stylesList;
    }

    public void setStylesList(List<String> stylesList) {
        this.stylesList = stylesList != null ? List.copyOf(stylesList) : Collections.emptyList();
    }

    public void setArtistPosts(List<Post> artistPosts) {

        this.artistPosts = artistPosts;
    }

    public List<Post> getArtistPosts() {
        return artistPosts != null ? artistPosts : Collections.emptyList();
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public java.util.List<app.entity.Post> getPosts() {
        return getArtistPosts();
    }

    public static final class WorkLocation {
        private final String address;
        private final double longitude;
        private final double latitude;

        public WorkLocation(String address, double longitude, double latitude) {
            this.address = address;
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }

}
