package app.controller.workspace;

import app.entity.CameraState;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Encapsulates orbit, pan, zoom, and reset behaviour for the workspace camera.
 */
public final class WorkspaceCamera {
    private final PerspectiveCamera camera;
    private final double minDistance;
    private final double maxDistance;
    private final double orbitSensitivity;
    private final double panSensitivity;
    private final double pitchMin;
    private final double pitchMax;

    private static final double DEFAULT_YAW = 180.0;
    private static final double DEFAULT_PITCH = -5.0;

    private final DoubleProperty yaw = new SimpleDoubleProperty(DEFAULT_YAW);
    private final DoubleProperty pitch = new SimpleDoubleProperty(DEFAULT_PITCH);
    private static final double DEFAULT_DISTANCE = 3200.0;

    private final DoubleProperty distance = new SimpleDoubleProperty(DEFAULT_DISTANCE);

    private Bounds bounds;
    private Point3D target = new Point3D(0, 0, 0);
    private double panX;
    private double panY;
    private double panZ;
    private double lastMouseX;
    private double lastMouseY;

    public WorkspaceCamera(PerspectiveCamera camera,
                           double minDistance,
                           double maxDistance,
                           double orbitSensitivity,
                           double panSensitivity,
                           double pitchMin,
                           double pitchMax) {
        this.camera = camera;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.orbitSensitivity = orbitSensitivity;
        this.panSensitivity = panSensitivity;
        this.pitchMin = pitchMin;
        this.pitchMax = pitchMax;

        camera.setNearClip(0.1);
        camera.setFarClip(20000);
        camera.setFieldOfView(35);
        yaw.addListener((obs, oldV, newV) -> updateCameraTransform());
        pitch.addListener((obs, oldV, newV) -> updateCameraTransform());
        distance.addListener((obs, oldV, newV) -> updateCameraTransform());
        updateCameraTransform();
    }

    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        if (bounds != null) {
            target = clampTarget(target, bounds);
        }
    }

    public void reset() {
        if (bounds == null) {
            return;
        }
        double centerX = (bounds.getMinX() + bounds.getMaxX()) * 0.5;
        double centerZ = (bounds.getMinZ() + bounds.getMaxZ()) * 0.5;
        double floor = bounds.getMinY();
        double height = bounds.getHeight();
        double targetY = floor + height * 0.50;

        target = new Point3D(centerX, targetY, centerZ);
        panX = panY = panZ = 0.0;
        yaw.set(DEFAULT_YAW);
        pitch.set(DEFAULT_PITCH);

        double maxDim = Math.max(bounds.getWidth(), Math.max(bounds.getHeight(), bounds.getDepth()));
        double fitDistance = maxDim * 1.6 + 650.0;
        distance.set(clamp(fitDistance, minDistance, maxDistance));
        updateCameraTransform();
    }

    public void handleMousePressed(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        }
    }

    public void handleMouseDragged(MouseEvent event) {
        double dx = event.getSceneX() - lastMouseX;
        double dy = event.getSceneY() - lastMouseY;
        lastMouseX = event.getSceneX();
        lastMouseY = event.getSceneY();

        boolean isShift = event.isShiftDown() || event.isControlDown();
        if (event.getButton() == MouseButton.PRIMARY && !isShift) {
            orbit(dx, dy);
        } else if (event.getButton() == MouseButton.SECONDARY || (event.getButton() == MouseButton.PRIMARY && isShift)) {
            pan(dx, dy);
        }
    }

    public void handleScroll(ScrollEvent event) {
        if (event.getDeltaY() == 0) {
            return;
        }
        double delta = event.getDeltaY() * 2.0;
        distance.set(clamp(distance.get() - delta, minDistance, maxDistance));
    }

    public void handleKey(KeyEvent event) {
        if (event.getCode() == KeyCode.SPACE) {
            reset();
        }
    }

    public CameraState snapshot() {
        return new CameraState(
            yaw.get(),
            pitch.get(),
            distance.get(),
            target.getX(),
            target.getY(),
            target.getZ(),
            panX,
            panY,
            panZ
        );
    }

    public void apply(CameraState state) {
        if (state == null) {
            return;
        }
        Point3D desiredTarget = new Point3D(state.targetX(), state.targetY(), state.targetZ());
        target = bounds != null ? clampTarget(desiredTarget, bounds) : desiredTarget;
        panX = state.panX();
        panY = state.panY();
        panZ = state.panZ();
        yaw.set(normalizeAngle(state.yaw()));
        pitch.set(clamp(state.pitch(), pitchMin, pitchMax));
        distance.set(clamp(state.distance(), minDistance, maxDistance));
        updateCameraTransform();
    }

    private void orbit(double dx, double dy) {
        yaw.set(normalizeAngle(yaw.get() - dx * orbitSensitivity));
        pitch.set(clamp(pitch.get() - dy * orbitSensitivity, pitchMin, pitchMax));
    }

    private void pan(double dx, double dy) {
        if (bounds == null) {
            return;
        }
        double factor = distance.get() / 800.0;
        double yawRadians = Math.toRadians(yaw.get());
        double cosYaw = Math.cos(yawRadians);
        double sinYaw = Math.sin(yawRadians);

        double rightX = cosYaw;
        double rightZ = -sinYaw;
        double forwardX = sinYaw;
        double forwardZ = cosYaw;

        panX -= dx * panSensitivity * factor * rightX;
        panZ -= dx * panSensitivity * factor * rightZ;
        panX += dy * panSensitivity * factor * forwardX * 0.6;
        panZ += dy * panSensitivity * factor * forwardZ * 0.6;
        panY += dy * panSensitivity * factor;
        updateCameraTransform();
    }

    private void updateCameraTransform() {
        double clampedDistance = clamp(distance.get(), minDistance, maxDistance);
        if (clampedDistance != distance.get()) {
            distance.set(clampedDistance);
            return;
        }
        camera.getTransforms().setAll(
            new Translate(target.getX(), target.getY(), target.getZ()),
            new Rotate(yaw.get(), Rotate.Y_AXIS),
            new Rotate(pitch.get(), Rotate.X_AXIS),
            new Translate(0, 0, -clampedDistance),
            new Translate(panX, panY, panZ)
        );
    }

    private Point3D clampTarget(Point3D candidate, Bounds bounds) {
        double centerX = (bounds.getMinX() + bounds.getMaxX()) * 0.5;
        double centerZ = (bounds.getMinZ() + bounds.getMaxZ()) * 0.5;
        double widthHalf = bounds.getWidth() * 0.5;
        double depthHalf = bounds.getDepth() * 0.5;
        double minY = bounds.getMinY() + bounds.getHeight() * 0.2;
        double maxY = bounds.getMinY() + bounds.getHeight() * 0.9;

        double clampedX = clamp(candidate.getX(), centerX - widthHalf, centerX + widthHalf);
        double clampedZ = clamp(candidate.getZ(), centerZ - depthHalf, centerZ + depthHalf);
        double clampedY = clamp(candidate.getY(), minY, maxY);
        return new Point3D(clampedX, clampedY, clampedZ);
    }

    private double clamp(double value, double min, double max) {
        return Math.clamp(value, min, max);
    }

    private double normalizeAngle(double angle) {
        double mod = angle % 360.0;
        return mod < 0 ? mod + 360.0 : mod;
    }
}
