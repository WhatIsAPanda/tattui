package app.entity;

import javafx.scene.image.Image;

/**
 * Domain entity representing a tattoo decal to be applied to the model texture.
 */
public class Tattoo {
    public double u;
    public double v;
    public double scale;
    public double rotation;
    public double alpha;
    public Image image;

    public Tattoo(double u, double v, Image image, double scale, double rotation, double alpha) {
        this.u = u;
        this.v = v;
        this.image = image;
        this.scale = scale;
        this.rotation = rotation;
        this.alpha = alpha;
    }
}
