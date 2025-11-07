package app.entity;

import javafx.scene.image.Image;
import java.util.Objects;

public final class Tattoo {
    private final double u;
    private final double v;
    private final double scale;
    private final double rotation;
    private final double alpha;
    private final Image image;
    public Tattoo(double u, double v, Image image, double scale, double rotation, double alpha) {
        this.u = clamp01(u); this.v = clamp01(v);
        this.scale = requirePositive(scale, "scale");
        this.rotation = rotation; this.alpha = clamp01(alpha);
        this.image = Objects.requireNonNull(image, "image");
    }
    public double u() { return u; } public double v() { return v; }
    public double scale() { return scale; } public double rotation() { return rotation; }
    public double alpha() { return alpha; } public Image image() { return image; }
    public Tattoo withUV(double uu, double vv){ return new Tattoo(uu, vv, image, scale, rotation, alpha); }
    public Tattoo withScale(double s){ return new Tattoo(u, v, image, s, rotation, alpha); }
    public Tattoo withRotation(double r){ return new Tattoo(u, v, image, scale, r, alpha); }
    public Tattoo withAlpha(double a){ return new Tattoo(u, v, image, scale, rotation, a); }
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
