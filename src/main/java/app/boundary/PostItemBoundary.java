package app.boundary;

import app.entity.Post;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;

public class PostItemBoundary {
    @FXML
    private ImageView postImageView;
    private Post post;

    public void setPost(Post post) {
        this.post = post;
        postImageView.setImage(post.getImage());
    }

    @FXML
    public void postItemClicked() {
        System.out.println("Post item clicked");
    }

    @FXML
    public void postItemMouseEntered() {
        postImageView.setOpacity(0.2);
    }

    @FXML
    public void postItemMouseExited() {
        postImageView.setOpacity(1);

    }

}
