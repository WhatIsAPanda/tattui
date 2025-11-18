package app.entity;

/**
 * Simple DTO representing a review left by an account for an artist.
 */
public class Review {
    private final int reviewId;
    private final int revieweeId;
    private final String pictureUrl;
    private final String reviewText;
    private final int rating;
    private final Reviewer reviewer;

    public Review(int reviewId, int revieweeId, String pictureUrl, String reviewText, int rating, Reviewer reviewer) {
        this.reviewId = reviewId;
        this.revieweeId = revieweeId;
        this.pictureUrl = pictureUrl;
        this.reviewText = reviewText;
        this.rating = rating;
        this.reviewer = reviewer;
    }

    public int getReviewId() {
        return reviewId;
    }

    public int getReviewerId() {
        return reviewer != null ? reviewer.id : -1;
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
        return reviewer != null ? reviewer.name : null;
    }

    public String getReviewerPicture() {
        return reviewer != null ? reviewer.pictureUrl : null;
    }

    public static final class Reviewer {
        private final int id;
        private final String name;
        private final String pictureUrl;

        public Reviewer(int id, String name, String pictureUrl) {
            this.id = id;
            this.name = name;
            this.pictureUrl = pictureUrl;
        }
    }
}
