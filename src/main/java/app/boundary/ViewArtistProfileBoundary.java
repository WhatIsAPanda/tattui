package app.boundary;

import app.entity.DatabaseConnector;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import java.sql.SQLException;

public class ViewArtistProfileBoundary extends BaseProfileBoundary {

    @FXML private Label artistNameField;
    @FXML private Circle profilePicture;
    @FXML private Text biographyField;
    @FXML private GridPane postsPanel;

    private Profile profile;

    @FXML
    public void setProfile(Profile profile) {
        try {
            this.profile = DatabaseConnector.getFullProfile(profile);
            loadProfile();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadProfile() {
        populateProfileCommon(profile, profilePicture, biographyField, artistNameField);
        populatePosts(postsPanel, profile.getArtistPosts());
    }
}
