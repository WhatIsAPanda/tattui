package tattui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FXMLLoadTest {

    @BeforeAll
    static void setupJavaFX() {
        JavaFXTestHelper.initToolkit(); // Runs once before any test
    }

    @Test
    void testRootFXMLLoads() {
        assertDoesNotThrow(() -> {
            Parent root = FXMLLoader.load(getClass().getResource("/tattui/fxml/Root.fxml"));
            assertNotNull(root);
        });
    }

    @Test
    void testGalleryFXMLLoads() {
        assertDoesNotThrow(() -> {
            Parent root = FXMLLoader.load(getClass().getResource("/tattui/fxml/Gallery.fxml"));
            assertNotNull(root);
        });
    }

    @Test
    void testLoginpageFXMLLoads() {
        assertDoesNotThrow(() -> {
            Parent root = FXMLLoader.load(getClass().getResource("/tattui/fxml/Loginpage.fxml"));
            assertNotNull(root);
        });
    }

    @Test
    void testTaskbarFXMLLoads() {
        assertDoesNotThrow(() -> {
            Parent root = FXMLLoader.load(getClass().getResource("/tattui/fxml/taskbar.fxml"));
            assertNotNull(root);
        });
    }

    @Test
    void testStructuredHomeFXMLLoads() {
        assertDoesNotThrow(() -> {
            Parent root = FXMLLoader.load(getClass().getResource("/tattui/fxml/StructuredHome.fxml"));
            assertNotNull(root);
        });
    }

    @Test
    void testStructuredPaneFXMLLoads() {
        assertDoesNotThrow(() -> {
            Parent root = FXMLLoader.load(getClass().getResource("/tattui/fxml/StructuredPane.fxml"));
            assertNotNull(root);
        });
    }

    @Test
    void testViewProfileFXMLLoads() {
        assertDoesNotThrow(() -> {
            Parent root = FXMLLoader.load(getClass().getResource("/tattui/fxml/viewProfile.fxml"));
            assertNotNull(root);
        });
    }
    // Add more screens as needed
}