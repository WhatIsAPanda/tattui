package app.boundary;

import app.entity.DatabaseConnector;
import app.entity.Profile;
import app.entity.Review;
import app.boundary.ReviewsDialogBoundary;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class EditArtistProfileBoundary extends BaseProfileBoundary {

    @FXML
    private Circle profilePicture;
    @FXML
    private TextArea biographyField;
    @FXML
    private Label artistNameField;
    @FXML
    private Label averageRatingLabel;
    @FXML
    private GridPane postsPanel;
    @FXML
    private TextField longitudeField;
    @FXML
    private TextField latitudeField;

    private Profile profile;
    private List<Review> reviews = List.of();

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
        refreshReviews();
    }

    private void refreshReviews() {
        if (profile == null) {
            return;
        }
        try {
            reviews = DatabaseConnector.loadReviews(profile.getAccountId());
        } catch (SQLException e) {
            e.printStackTrace();
            reviews = List.of();
        }
        updateAverageRating();
    }

    private void updateAverageRating() {
        if (averageRatingLabel == null) {
            return;
        }
        if (reviews.isEmpty()) {
            averageRatingLabel.setText("No reviews");
            return;
        }
        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        averageRatingLabel.setText(String.format("%.1f / 5", average));
    }

    @FXML
    private void openReviewPage() {
        if (profile == null) {
            return;
        }
        refreshReviews();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/ReviewsDialog.fxml"));
            Parent root = loader.load();
            ReviewsDialogBoundary dialogController = loader.getController();
            dialogController.setData(profile, reviews);

            Stage stage = new Stage();
            stage.setTitle("Reviews - @" + profile.getUsername());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
