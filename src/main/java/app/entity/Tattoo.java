package app.entity;

import javafx.scene.image.Image;
import java.util.Objects;

public final class Tattoo {
    private final double u;
    private final double v;
    private final double widthScale;
    private final double heightScale;
    private final double rotation;
    private final double alpha;
    private final Image image;

    public Tattoo(double u, double v, Image image, double widthScale, double heightScale, double rotation, double alpha) {
        this.u = clamp01(u);
        this.v = clamp01(v);
        this.widthScale = requirePositive(widthScale, "widthScale");
        this.heightScale = requirePositive(heightScale, "heightScale");
        this.rotation = rotation;
        this.alpha = clamp01(alpha);
        this.image = Objects.requireNonNull(image, "image");
    }

    public double u() { return u; }
    public double v() { return v; }
    public double widthScale() { return widthScale; }
    public double heightScale() { return heightScale; }
    public double rotation() { return rotation; }
    public double alpha() { return alpha; }
    public Image image() { return image; }

    /**
     * Returns the average of the width and height scale components.
     * Provided for backwards compatibility with earlier single-scale semantics.
     */
    public double scale() {
        return (widthScale + heightScale) * 0.5;
    }

    public Tattoo withUV(double uu, double vv) {
        return new Tattoo(uu, vv, image, widthScale, heightScale, rotation, alpha);
    }

    public Tattoo withScale(double s) {
        return withUniformScale(s);
    }

    public Tattoo withUniformScale(double s) {
        return new Tattoo(u, v, image, s, s, rotation, alpha);
    }

    public Tattoo withWidthScale(double s) {
        return new Tattoo(u, v, image, s, heightScale, rotation, alpha);
    }

    public Tattoo withHeightScale(double s) {
        return new Tattoo(u, v, image, widthScale, s, rotation, alpha);
    }

    public Tattoo withRotation(double r) {
        return new Tattoo(u, v, image, widthScale, heightScale, r, alpha);
    }

    public Tattoo withAlpha(double a) {
        return new Tattoo(u, v, image, widthScale, heightScale, rotation, a);
    }
    private static double clamp01(double x) {
        if (x < 0) {
            return 0;
        } else if (x > 1) {
            return 1;
        } else {
            return x;
        }
    }
    private static double requirePositive(double x, String n) {
        if(x<=0) {
            throw new IllegalArgumentException(n+" > 0");
        }
        return x;
    }
}
