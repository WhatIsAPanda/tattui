package app.entity;

/**
 * Simple DTO representing a review left by an account for an artist.
 */
public class Review {
    private final int reviewId;
    private final int reviewerId;
    private final int revieweeId;
    private final String pictureUrl;
    private final String reviewText;
    private final int rating;
    private final String reviewerName;
    private final String reviewerPicture;

    public Review(int reviewId, int reviewerId, int revieweeId, String pictureUrl, String reviewText, int rating,
            String reviewerName, String reviewerPicture) {
        this.reviewId = reviewId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.pictureUrl = pictureUrl;
        this.reviewText = reviewText;
        this.rating = rating;
        this.reviewerName = reviewerName;
        this.reviewerPicture = reviewerPicture;
    }

    public int getReviewId() {
        return reviewId;
    }

    public int getReviewerId() {
        return reviewerId;
    }

    public int getRevieweeId() {
        return revieweeId;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public String getReviewText() {
        return reviewText;
    }

    public int getRating() {
        return rating;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public String getReviewerPicture() {
        return reviewerPicture;
    }
}
