package app.boundary;

import app.entity.LoggedInUser;
import app.entity.Post;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.util.List;

public class ViewMyProfileBoundary {
    @FXML
    private Circle profilePicture;
    @FXML
    private Text biographyField;
    @FXML
    private Text artistNameField1;
    @FXML
    private Text artistNameField2;
    @FXML
    private GridPane postsPanel;
    @FXML
    private Profile profile;

    @FXML
    public void initialize() {

    }

    @FXML
    public void setProfile(Profile userProfile) {
        this.profile = userProfile;
        if(artistNameField1 == null) {
            System.out.println("ERROR");
        }

        loadProfile();
    }
    @FXML
    public void loadProfile() {
        System.out.println("artistNameField1 = " + artistNameField1);
        System.out.println("profilePicture = " + profilePicture);
        System.out.println("postsPanel = " + postsPanel);
        System.out.println(profile.getUsername());
        System.out.println(profile.getPassword());
        artistNameField1.setText("@" + profile.getUsername());
        artistNameField2.setText("@" + profile.getUsername());
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
                ImageView imgView = new ImageView(posts.get(imagesProcessed).image);
                imgView.setFitHeight(250);
                imgView.setFitWidth(179);
                imgView.setPreserveRatio(false);

                postsPanel.add(imgView, col, row);
                imagesProcessed++;
            }
        }
    }

}
