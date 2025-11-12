package app.entity;

import javafx.scene.image.Image;

public class Post {
    private int id;
    private String caption;
    private String postURL;
    private Image image;

    public Post(int id, String caption, String postURL) {
        this.id = id;
        this.caption = caption;
        this.postURL = postURL;

        if (postURL != null && !postURL.isBlank()) {
            try {
                this.image = new Image(postURL, true);
            } catch (Exception e) {
                e.printStackTrace();
                this.image = null;
            }
        } else {
            this.image = null;
        }
    }

    public Image getImage() {
        return image;
    }

    public String getCaption() {
        return caption;
    }

    public String getPostURL() {
        return postURL;
    }

    public int getId() {
        return id;
    }
}
