package app.entity;

import javafx.scene.image.Image;

public class Post {
    private int id;
    private String caption;
    private String postURL;
    private int postOwnerId;
    private String keywords;

    // Lazy image; don't create during tests/headless contexts
    private volatile Image image;

    // Optional headless switch for tests (default false)
    private static volatile boolean HEADLESS = Boolean.getBoolean("HEADLESS_TESTS");

    public static void setHeadless(boolean v) { HEADLESS = v; }

    public Post(int id, String caption, String postURL) {
        this.id = id;
        this.caption = caption;
        this.postURL = postURL;
        this.keywords = null;
        // leave author-related fields at default/null if your 5-arg version added them
    }


    public Post(int id, String caption, String postURL, int postOwnerId, String keywords) {
        this.id = id;
        this.caption = caption;
        this.postURL = postURL;
        this.postOwnerId = postOwnerId;
        this.keywords = keywords;
        // IMPORTANT: do NOT create Image here; we’re in plain Java/JUnit often.
        // The UI can call getImage() when a JavaFX toolkit exists.
    }

    public Image getImage() {
        if (image == null && !HEADLESS && postURL != null && !postURL.isBlank()) {
            try {
                // backgroundLoading=true; okay when JavaFX runtime is present
                image = new Image(postURL, true);
            } catch (Exception ignored) {
                // leave image null
            }
        }
        return image;
    }

    public String getCaption() { return caption; }
    public String getPostURL() { return postURL; }
    public int getId() { return id; }
    public int getPostOwnerId() { return postOwnerId; }
    public String getKeywords() { return keywords; }
}
