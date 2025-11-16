package app.boundary;

import app.entity.Post;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

public class EditMyProfileBoundary {
    @FXML
    private Circle profilePicture;
    @FXML
    private TextArea biographyField;
    @FXML
    private TextField artistNameField;
    @FXML
    private GridPane postsPanel;
    @FXML
    private Profile profile;

    @FXML
    public void setProfile(Profile userProfile) {
        this.profile = userProfile;
        if(artistNameField == null) {
            System.out.println("ERROR");
        }

        loadProfile();
    }
    @FXML
    public void loadProfile() {
        artistNameField.setText("@" + profile.getUsername());
        Image profile_picture = profile.getProfile_picture_image();
        ImagePattern pfpImagePattern = new ImagePattern(profile_picture);
        profilePicture.setFill(pfpImagePattern);
        biographyField.setText(profile.getBiography());

        List<Post> posts =  profile.getPosts();
        int imagesProcessed = 0;
        int row = -1;
        while(imagesProcessed < posts.size()) {
            row++;
            RowConstraints constraints = new RowConstraints();
            constraints.setMaxHeight(250);
            constraints.setMinHeight(250);
            postsPanel.getRowConstraints().add(constraints);
            for(int col = 0; col < postsPanel.getColumnCount(); col++) {
                if(imagesProcessed == posts.size()) {
                    break;
                }
                ImageView imgView = new ImageView(posts.get(imagesProcessed).getImage());
                imgView.setFitHeight(250);
                imgView.setFitWidth(179);
                imgView.setPreserveRatio(false);

                postsPanel.add(imgView, col, row);
                imagesProcessed++;
            }
        }
    }

}
