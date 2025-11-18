package app.entity;

import javafx.scene.image.Image;

import java.util.Collections;
import java.util.List;

public class Profile {
    private int account_id;
    private String username;
    public String biography;
    private String profile_picture_url;
    public Image profile_picture;
    private String work_address;
    public double work_longitude;
    public double work_latitude;
    private List<String> styles_list;
    private List<Post> artistPosts;

    public Profile(int account_id, String username, String profile_picture_url, String biography, String work_address,
            double work_longitude, double work_latitude, List<String> styles_list) {
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

    public String getProfilePictureURL() {
        return profile_picture_url;
    }

    public void setProfilePicture(String profilePictureUrl, Image image) {
        this.profile_picture_url = profilePictureUrl;
        this.profile_picture = image;
    }

    public String getAddress() {
        return work_address;
    }

    public List<String> getStylesList() {
        return styles_list;
    }

    public void setArtistPosts(List<Post> artistPosts) {

        this.artistPosts = artistPosts;
    }

    public List<Post> getArtistPosts() {
        return artistPosts != null ? artistPosts : Collections.emptyList();
    }

    // TEMP shim to satisfy legacy probe tests (DbProfilesProbeTest etc.).
    // TODO(Adnan): remove once probe tests are updated to new data model.
    // public java.util.List<app.entity.Post> getPosts() { return null; }

    // ---- Legacy compatibility shims (safe to keep; remove later if not needed) ----
    public String getBiography() {
        return biography;
    }

    public java.util.List<app.entity.Post> getPosts() {
        // Older probes used getPosts(); our model uses getArtistPosts()
        return getArtistPosts();
    }


}
