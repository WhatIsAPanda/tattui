package app.boundary;

import app.entity.Profile;
import app.entity.Review;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.util.List;

/**
 * Simple dialog controller that renders an artist's reviews and average rating.
 */
public class ReviewsDialogBoundary {

    private static final String DEFAULT_AVATAR = "/icons/artist_raven.jpg";

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
            HBox root = new HBox(12);
            root.setAlignment(Pos.TOP_LEFT);
            root.setPadding(new Insets(8, 0, 8, 0));

            Circle avatarCircle = new Circle(24);
            avatarCircle.setStroke(Color.WHITE);
            avatarCircle.setStrokeWidth(1.0);
            String picture = review.getReviewerPicture();
            Image avatarImg;
            if (picture != null && !picture.isBlank()) {
                avatarImg = new Image(picture, 96, 96, true, true);
            } else {
                avatarImg = new Image(DEFAULT_AVATAR, 96, 96, true, true);
            }
            avatarCircle.setFill(new ImagePattern(avatarImg));

            VBox content = new VBox(4);
            Label nameLabel = new Label(
                    (review.getReviewerName() == null || review.getReviewerName().isBlank())
                            ? "Anonymous" : review.getReviewerName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
            Label ratingLabel = new Label("Rating: " + review.getRating() + "/5");
            ratingLabel.setStyle("-fx-text-fill: #cbd5f5;");
            Label textLabel = new Label(review.getReviewText());
            textLabel.setWrapText(true);
            textLabel.setStyle("-fx-text-fill: #e2e8f0;");
            content.getChildren().addAll(nameLabel, ratingLabel, textLabel);

            if (review.getPictureUrl() != null && !review.getPictureUrl().isBlank()) {
                ImageView reviewImage = new ImageView(new Image(review.getPictureUrl(), 220, 220, true, true));
                reviewImage.setPreserveRatio(true);
                reviewImage.setFitWidth(220);
                reviewImage.setSmooth(true);
                reviewImage.getStyleClass().add("review-photo");
                content.getChildren().add(reviewImage);
            }
            HBox.setHgrow(content, Priority.ALWAYS);

            root.getChildren().addAll(avatarCircle, content);
            setGraphic(root);
            setText(null);
        }
    }
}
