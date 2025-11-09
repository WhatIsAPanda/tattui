package app.entity;

import javafx.scene.image.Image;

public class Post {
    public int id;
    public String caption;
    public String url;
    public Image image;

    public Post(int id, String caption, String url) {
        image = new Image(url);
        this.caption = caption;
        this.url = url;
        this.id = id;
    }


}
