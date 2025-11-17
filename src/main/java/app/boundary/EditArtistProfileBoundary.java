package app.boundary;

import app.entity.DatabaseConnector;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.input.MouseEvent;

import java.sql.SQLException;

import javax.xml.crypto.Data;

public class EditArtistProfileBoundary extends BaseProfileBoundary {

    @FXML private Circle profilePicture;
    @FXML private TextArea biographyField;
    @FXML private Label artistNameField;
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
    @FXML
    private void handleBack() {
        System.out.println("Back clicked");
    }

    @FXML
    private void handleProfileClick(javafx.scene.input.MouseEvent event) {
        System.out.println("Profile picture clicked");
        loadProfile();
    }
    @FXML 
    private void handleSaveChanges(javafx.event.ActionEvent event) {
        DatabaseConnector.modifyUser(profile);

    }
    private void loadProfile() {
        populateProfileCommon(profile, profilePicture, biographyField, artistNameField);
        populatePosts(postsPanel, profile.getArtistPosts());
    }
}
