package app.entity;

import javafx.scene.image.Image;

public class Post {
    private int id;
    private String caption;
    private String url;
    private Image image;

    public Post(int id, String caption, String url) {
        image = new Image(url);
        this.caption = caption;
        this.url = url;
        this.id = id;
    }
}
