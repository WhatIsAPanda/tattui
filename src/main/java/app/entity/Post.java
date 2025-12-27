package app.entity;

import javafx.scene.image.Image;

public class Post {
    private int id;
    private String caption;
    private String postURL;
    private String ownerUsername;
    private String keyWords;
    private int postOwnerId;

    private Image image;

    public Post(int id, String caption, String postURL, int postOwnerId, String keyWords, String ownerUsername) {
        this.id = id;
        this.caption = caption;
        this.postURL = postURL;
        this.postOwnerId = postOwnerId;
        this.keyWords = keyWords;
        this.ownerUsername = ownerUsername;

    }

    public Image getImage() {
        if (image == null && postURL != null && !postURL.isBlank()) {
            try {
                // backgroundLoading=true; okay when JavaFX runtime is present
                image = new Image(postURL, true);
            } catch (Exception ignored) {
                // leave image null
            }
        }
        return image;
    }

    public String getCaption() { return caption; }
    public String getPostURL() { return postURL; }
    public int getId() { return id; }
}
