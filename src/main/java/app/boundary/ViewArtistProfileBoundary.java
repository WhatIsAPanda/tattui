package app.boundary;

import app.controller.RootController;
import app.entity.DatabaseConnector;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import java.sql.SQLException;

public class ViewArtistProfileBoundary extends BaseProfileBoundary implements RootController.PageAware {

    @FXML
    private Label artistNameField;
    @FXML
    private Circle profilePicture;
    @FXML
    private Label biographyField;
    @FXML
    private GridPane postsPanel;

    private Profile profile;
    private java.util.function.Consumer<String> pageRequest;

    @Override
    public void setOnPageRequest(java.util.function.Consumer<String> handler) {
        this.pageRequest = handler;
    }

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

    @FXML
    private void handleLeaveReview() {
        if (pageRequest != null) {
            pageRequest.accept("postReview");
        }
    }
}
