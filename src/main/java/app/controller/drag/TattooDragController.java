package app.controller.drag;

import app.entity.Tattoo;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.shape.MeshView;

import java.util.List;

/**
 * Handles tattoo placement and dragging over model UVs.
 */
public class TattooDragController {
    private final List<Tattoo> tattoos;
    private final Runnable repaintCallback;
    private Tattoo selectedTattoo;
    private Image pendingTattoo;

    public TattooDragController(List<Tattoo> tattoos, Runnable repaintCallback) {
        this.tattoos = tattoos;
        this.repaintCallback = repaintCallback;
    }

    public void setPendingTattoo(Image img) {
        this.pendingTattoo = img;
    }

    public Tattoo getSelectedTattoo() {
        return selectedTattoo;
    }

    public void handlePress(MouseEvent e, double size, double opacity, double rotation) {
        if (e.getButton() == MouseButton.SECONDARY) {
            tattoos.clear();
            selectedTattoo = null;
            repaintCallback.run();
            return;
        }

        if (e.getButton() != MouseButton.PRIMARY || (selectedTattoo == null && pendingTattoo == null)) return;
        PickResult r = e.getPickResult();
        if (r == null || !(r.getIntersectedNode() instanceof MeshView)) return;
        Point2D uv = r.getIntersectedTexCoord();
        if (uv == null) return;

        double u = clamp(uv.getX(), 0, 1);
        double v = clamp(uv.getY(), 0, 1);

        if (selectedTattoo == null) {
            selectedTattoo = new Tattoo(u, v, pendingTattoo, size, rotation, opacity);
            tattoos.clear();
            tattoos.add(selectedTattoo);
            pendingTattoo = null;
        } else {
            selectedTattoo.u = u;
            selectedTattoo.v = v;
            if (pendingTattoo != null) {
                selectedTattoo.image = pendingTattoo;
                selectedTattoo.scale = size;
                selectedTattoo.rotation = rotation;
                selectedTattoo.alpha = opacity;
                pendingTattoo = null;
            }
        }

        repaintCallback.run();
        e.consume();
    }

    public void handleDrag(MouseEvent e) {
        if (selectedTattoo == null || !e.isPrimaryButtonDown()) return;
        PickResult r = e.getPickResult();
        if (r == null || !(r.getIntersectedNode() instanceof MeshView)) return;
        Point2D uv = r.getIntersectedTexCoord();
        if (uv == null) return;

        selectedTattoo.u = clamp(uv.getX(), 0, 1);
        selectedTattoo.v = clamp(uv.getY(), 0, 1);
        repaintCallback.run();
        e.consume();
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
