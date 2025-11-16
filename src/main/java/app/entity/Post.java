package app.entity;

import javafx.scene.image.Image;

public class Post {
    private int id;
    private String caption;
    private String postURL;
    private String keyWords;
    private int postOwnerId;

    // Lazy image; don't create during tests/headless contexts
    private volatile Image image;

    // Optional headless switch for tests (default false)
    private static volatile boolean HEADLESS = Boolean.getBoolean("HEADLESS_TESTS");

    public static void setHeadless(boolean v) { HEADLESS = v; }

    public Post(int id, String caption, String postURL, int postOwnerId, String keyWords) {
        this.id = id;
        this.caption = caption;
        this.postURL = postURL;
        this.postOwnerId = postOwnerId;
        this.keyWords = keyWords;
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
}
