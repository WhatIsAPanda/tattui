package app.boundary;

import app.entity.DatabaseConnector;
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

import java.sql.SQLException;
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
    
    private Profile profile;

    @FXML
    public void setProfile(Profile profile) {
        try {
            this.profile = DatabaseConnector.getFullProfile(profile);
            loadProfile();
        }
        catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    @FXML
    public void loadProfile() {
        if (artistNameField == null) {
                System.out.println("FXML not initialized — make sure EditMyProfile.fxml defines fx:id=\"artistNameField\"");
                return;
        }
        artistNameField.setText("@" + profile.getUsername());
        Image profile_picture = profile.getProfilePicture();
        ImagePattern pfpImagePattern = new ImagePattern(profile_picture);
        profilePicture.setFill(pfpImagePattern);
        biographyField.setText(profile.getBiography());

        List<Post> posts =  profile.getArtistPosts();
        int imagesProcessed = 0;
        int row = -1;
        if (posts != null && !posts.isEmpty()) {
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

}
