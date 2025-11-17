package app.boundary;

import app.entity.DatabaseConnector;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import java.sql.SQLException;

public class EditMyProfileBoundary extends BaseProfileBoundary {

    @FXML private Circle profilePicture;
    @FXML private TextArea biographyField;
    @FXML private TextField artistNameField;
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
