package app.boundary;

import app.entity.DatabaseConnector;
import app.entity.Profile;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.TextField;

import java.sql.SQLException;

import javax.xml.crypto.Data;

public class EditArtistProfileBoundary extends BaseProfileBoundary {

    @FXML
    private Circle profilePicture;
    @FXML
    private TextArea biographyField;
    @FXML
    private Label artistNameField;
    @FXML
    private GridPane postsPanel;
    @FXML
    private TextField longitudeField;
    @FXML
    private TextField latitudeField;

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
    private void handleProfileClick() {
        System.out.println("Profile picture clicked");
        loadProfile();
    }

    @FXML
    private void handleSaveChanges() {
        try {
            profile.biography = biographyField.getText();
            profile.work_latitude = Double.parseDouble(longitudeField.getText());
            profile.work_longitude = Double.parseDouble(latitudeField.getText());
            DatabaseConnector.modifyUser(profile);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void loadProfile() {
        populateProfileCommon(profile, profilePicture, biographyField, artistNameField);
        populatePosts(postsPanel, profile.getArtistPosts());
    }
}
