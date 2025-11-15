package app.controller.workspace;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and loop tests for TattooWorkspace.
 *
 * Loop Testing:
 *  - rebuildTintedBase() has a nested loop over y and x.
 *  - We design tests so that:
 *      * the loops do not run at all (canvasWidth or canvasHeight <= 0),
 *      * they run for a 1x1 image,
 *      * they run for a multi-pixel image and correctly tint each pixel.
 */
class TattooWorkspaceTest {

    // ---- Reflection helpers -------------------------------------------------

    private void setIntField(Object target, String fieldName, int value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    private void setObjectField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getObjectField(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        Object value = f.get(target);
        return type.cast(value);
    }

    private void invokeRebuildTintedBase(TattooWorkspace workspace) throws Exception {
        Method m = TattooWorkspace.class.getDeclaredMethod("rebuildTintedBase");
        m.setAccessible(true);
        m.invoke(workspace);
    }

    // ---- Tests --------------------------------------------------------------

    @Test
    void rebuildTintedBase_doesNothingWhenCanvasSizeNonPositive() throws Exception {
        TattooWorkspace workspace = new TattooWorkspace();

        // Pretend the canvas has invalid size so the loops are skipped
        setIntField(workspace, "canvasWidth", 0);
        setIntField(workspace, "canvasHeight", 10);

        // Give tintedBase an existing value to verify it is not changed
        WritableImage initial = new WritableImage(2, 2);
        setObjectField(workspace, "tintedBase", initial);

        invokeRebuildTintedBase(workspace);

        WritableImage result = getObjectField(workspace, "tintedBase", WritableImage.class);
        assertSame(initial, result, "tintedBase should remain unchanged when width/height <= 0");
    }

    @Test
    void rebuildTintedBase_fillsSolidWhenNoBaseTexture_singlePixel() throws Exception {
        TattooWorkspace workspace = new TattooWorkspace();

        // 1x1 canvas – loop executes exactly once in each dimension
        setIntField(workspace, "canvasWidth", 1);
        setIntField(workspace, "canvasHeight", 1);

        // No baseTemplate -> uses fillSolid() with current skinTone
        setObjectField(workspace, "baseTemplate", null);
        setObjectField(workspace, "skinTone", Color.RED);

        invokeRebuildTintedBase(workspace);

        WritableImage tinted = getObjectField(workspace, "tintedBase", WritableImage.class);
        assertNotNull(tinted, "tintedBase should be created");
        assertEquals(1, tinted.getWidth(), 1e-6);
        assertEquals(1, tinted.getHeight(), 1e-6);

        PixelReader reader = tinted.getPixelReader();
        Color c = reader.getColor(0, 0);
        assertEquals(Color.RED.getRed(), c.getRed(), 1e-6);
        assertEquals(Color.RED.getGreen(), c.getGreen(), 1e-6);
        assertEquals(Color.RED.getBlue(), c.getBlue(), 1e-6);
        assertEquals(Color.RED.getOpacity(), c.getOpacity(), 1e-6);
    }

    @Test
    void rebuildTintedBase_tintsExistingTemplate_multiPixel() throws Exception {
        TattooWorkspace workspace = new TattooWorkspace();

        // 2x1 canvas – outer loop (y) runs once, inner loop (x) runs twice
        int width = 2;
        int height = 1;
        setIntField(workspace, "canvasWidth", width);
        setIntField(workspace, "canvasHeight", height);

        // Build a simple baseTemplate with two different pixels
        WritableImage template = new WritableImage(width, height);
        PixelWriter writer = template.getPixelWriter();
        Color left = Color.color(0.5, 0.5, 0.5, 1.0);
        Color right = Color.color(1.0, 0.0, 0.0, 1.0);
        writer.setColor(0, 0, left);
        writer.setColor(1, 0, right);

        // skinTone scales RGB channels
        Color skinTone = Color.color(0.5, 1.0, 1.0, 1.0);
        setObjectField(workspace, "baseTemplate", template);
        setObjectField(workspace, "skinTone", skinTone);

        invokeRebuildTintedBase(workspace);

        WritableImage tinted = getObjectField(workspace, "tintedBase", WritableImage.class);
        assertNotNull(tinted, "tintedBase should be created");
        assertEquals(width, tinted.getWidth(), 1e-6);
        assertEquals(height, tinted.getHeight(), 1e-6);

        PixelReader r = tinted.getPixelReader();

        // Expected tinted colors: component-wise multiply sample * skinTone
        Color tintedLeft = r.getColor(0, 0);
        double tolerance = 1e-2;
        assertEquals(left.getRed() * skinTone.getRed(), tintedLeft.getRed(), tolerance);
        assertEquals(left.getGreen() * skinTone.getGreen(), tintedLeft.getGreen(), tolerance);
        assertEquals(left.getBlue() * skinTone.getBlue(), tintedLeft.getBlue(), tolerance);
        assertEquals(left.getOpacity(), tintedLeft.getOpacity(), 1e-6);

        Color tintedRight = r.getColor(1, 0);
        assertEquals(right.getRed() * skinTone.getRed(), tintedRight.getRed(), tolerance);
        assertEquals(right.getGreen() * skinTone.getGreen(), tintedRight.getGreen(), tolerance);
        assertEquals(right.getBlue() * skinTone.getBlue(), tintedRight.getBlue(), tolerance);
        assertEquals(right.getOpacity(), tintedRight.getOpacity(), 1e-6);
    }
}
