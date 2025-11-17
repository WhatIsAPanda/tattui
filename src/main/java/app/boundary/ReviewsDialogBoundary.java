package app.boundary;

import app.entity.Profile;
import app.entity.Review;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.util.List;

/**
 * Simple dialog controller that renders an artist's reviews and average rating.
 */
public class ReviewsDialogBoundary {

    @FXML
    private Label artistNameLabel;
    @FXML
    private Label averageRatingLabel;
    @FXML
    private ListView<Review> reviewListView;

    private Profile profile;
    private List<Review> reviews = List.of();

    @FXML
    private void initialize() {
        if (reviewListView != null) {
            reviewListView.setCellFactory(list -> new ReviewCell());
        }
    }

    public void setData(Profile profile, List<Review> reviews) {
        this.profile = profile;
        this.reviews = reviews != null ? reviews : List.of();
        if (artistNameLabel != null && profile != null) {
            artistNameLabel.setText("Reviews for @" + profile.getUsername());
        }
        if (reviewListView != null) {
            reviewListView.setItems(FXCollections.observableArrayList(this.reviews));
        }
        updateAverageRatingLabel();
    }

    private void updateAverageRatingLabel() {
        if (averageRatingLabel == null) {
            return;
        }
        if (reviews.isEmpty()) {
            averageRatingLabel.setText("No reviews yet");
            return;
        }
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        averageRatingLabel.setText(String.format("Average: %.1f / 5 (%d)", avg, reviews.size()));
    }

    private static final class ReviewCell extends ListCell<Review> {
        @Override
        protected void updateItem(Review review, boolean empty) {
            super.updateItem(review, empty);
            if (empty || review == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            String builder = "Rating: " + review.getRating() + "/5\n" + review.getReviewText();
            setText(builder);
        }
    }
}
