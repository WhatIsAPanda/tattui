package app.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class PostHeadlessTest {

    @Test
    void getImage_returnsNull_whenHeadless() {
        Post.setHeadless(true);
        Post p = new Post(1, "caption", "http://example.com/img.jpg");

        // In headless mode the JavaFX toolkit should not be triggered
        assertNull(p.getImage(), "Image should not load when headless is enabled");
    }
}
