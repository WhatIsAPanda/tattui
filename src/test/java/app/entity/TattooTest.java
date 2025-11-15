package app.entity;

import javafx.scene.image.WritableImage;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TattooTest {

    private Image newStubImage() {
        // Small, simple image is enough for the Tattoo constructor
        return new WritableImage(1, 1);
    }

    @Test
    void constructor_clampsUVAndAlphaToRange01() {
        Image img = newStubImage();

        // u, v and alpha are intentionally out of range to test clamping
        Tattoo tattoo = new Tattoo(-0.5, 1.5, img, 1.0, 2.0, 45.0, 1.5);

        assertEquals(0.0, tattoo.u(), 1e-6, "u should be clamped to 0");
        assertEquals(1.0, tattoo.v(), 1e-6, "v should be clamped to 1");
        assertEquals(1.0, tattoo.alpha(), 1e-6, "alpha should be clamped to 1");

        // Non-clamped values should pass through unchanged
        assertEquals(1.0, tattoo.widthScale(), 1e-6);
        assertEquals(2.0, tattoo.heightScale(), 1e-6);
        assertEquals(45.0, tattoo.rotation(), 1e-6);
        assertSame(img, tattoo.image());
    }

    @Test
    void constructor_negativeOrZeroScaleThrows() {
        Image img = newStubImage();

        // widthScale must be > 0
        assertThrows(IllegalArgumentException.class,
            () -> new Tattoo(0.5, 0.5, img, 0.0, 1.0, 0.0, 1.0),
            "widthScale == 0 should throw");

        // heightScale must be > 0
        assertThrows(IllegalArgumentException.class,
            () -> new Tattoo(0.5, 0.5, img, 1.0, -1.0, 0.0, 1.0),
            "heightScale < 0 should throw");
    }

    @Test
    void scale_returnsAverageOfWidthAndHeightScale() {
        Image img = newStubImage();

        Tattoo tattoo = new Tattoo(0.5, 0.5, img, 2.0, 4.0, 0.0, 1.0);

        // scale() is defined as average of widthScale and heightScale
        assertEquals(3.0, tattoo.scale(), 1e-6);
    }

    @Test
    void withers_createNewInstancesAndDoNotMutateOriginal() {
        Image img = newStubImage();

        Tattoo original = new Tattoo(0.5, 0.5, img, 1.0, 1.0, 0.0, 1.0);

        Tattoo scaled = original.withUniformScale(2.0);
        Tattoo widthScaled = original.withWidthScale(3.0);
        Tattoo heightScaled = original.withHeightScale(4.0);
        Tattoo rotated = original.withRotation(90.0);
        Tattoo transparent = original.withAlpha(0.25);

        // Original remains unchanged
        assertEquals(1.0, original.widthScale(), 1e-6);
        assertEquals(1.0, original.heightScale(), 1e-6);
        assertEquals(0.0, original.rotation(), 1e-6);
        assertEquals(1.0, original.alpha(), 1e-6);

        // New instances have the updated values
        assertEquals(2.0, scaled.widthScale(), 1e-6);
        assertEquals(2.0, scaled.heightScale(), 1e-6);

        assertEquals(3.0, widthScaled.widthScale(), 1e-6);
        assertEquals(1.0, widthScaled.heightScale(), 1e-6);

        assertEquals(1.0, heightScaled.widthScale(), 1e-6);
        assertEquals(4.0, heightScaled.heightScale(), 1e-6);

        assertEquals(90.0, rotated.rotation(), 1e-6);

        assertEquals(0.25, transparent.alpha(), 1e-6);
    }
}
