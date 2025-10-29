package app.controller.drag;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Handles orbiting, panning, and zooming of the 3D model.
 */
public class ModelDragController {
    private static final double ORBIT_SENSITIVITY = 0.3;
    private static final double PAN_SENSITIVITY = 0.5;
    private static final double ZOOM_SENSITIVITY = 2.0;
    private static final double MIN_DISTANCE = 300.0;
    private static final double MAX_DISTANCE = 6000.0;
    private static final double PITCH_MIN = -80.0;
    private static final double PITCH_MAX = 80.0;

    private final PerspectiveCamera camera;
    private final DoubleProperty yaw;
    private final DoubleProperty pitch;
    private final DoubleProperty distance;

    private Point3D cameraTarget;
    private Bounds bounds;

    public ModelDragController(PerspectiveCamera camera, DoubleProperty yaw, DoubleProperty pitch,
                               DoubleProperty distance, Point3D cameraTarget, Bounds bounds) {
        this.camera = camera;
        this.yaw = yaw;
        this.pitch = pitch;
        this.distance = distance;
        this.cameraTarget = cameraTarget;
        this.bounds = bounds;
    }

    public void onDrag(MouseEvent e, double dx, double dy, boolean orbitMode) {
        if (orbitMode) orbit(dx, dy);
        else pan(dx, dy);
    }

    public void onScroll(ScrollEvent e) {
        if (e.getDeltaY() == 0) return;
        double delta = e.getDeltaY() * ZOOM_SENSITIVITY;
        distance.set(clamp(distance.get() - delta, MIN_DISTANCE, MAX_DISTANCE));
        updateCameraTransform();
    }

    private void orbit(double dx, double dy) {
        yaw.set(normalizeAngle(yaw.get() - dx * ORBIT_SENSITIVITY));
        pitch.set(clamp(pitch.get() - dy * ORBIT_SENSITIVITY, PITCH_MIN, PITCH_MAX));
        updateCameraTransform();
    }

    private void pan(double dx, double dy) {
        if (bounds == null) return;
        double factor = distance.get() / 800.0;
        double newX = cameraTarget.getX() - dx * PAN_SENSITIVITY * factor;
        double newY = cameraTarget.getY() + dy * PAN_SENSITIVITY * factor;
        double newZ = cameraTarget.getZ() + dx * PAN_SENSITIVITY * factor * 0.6;
        cameraTarget = new Point3D(newX, newY, newZ);
        updateCameraTransform();
    }

    public void setBounds(Bounds b) { this.bounds = b; }

    public void setCameraTarget(Point3D p) { this.cameraTarget = p; }

    private void updateCameraTransform() {
        double d = clamp(distance.get(), MIN_DISTANCE, MAX_DISTANCE);
        camera.getTransforms().setAll(
            new Translate(cameraTarget.getX(), cameraTarget.getY(), cameraTarget.getZ()),
            new Rotate(yaw.get(), Rotate.Y_AXIS),
            new Rotate(pitch.get(), Rotate.X_AXIS),
            new Translate(0, 0, -d)
        );
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double normalizeAngle(double a) {
        double mod = a % 360.0;
        return mod < 0 ? mod + 360.0 : mod;
    }
}
