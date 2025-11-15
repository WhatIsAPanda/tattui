package app.entity;

public record CameraState(
    double yaw,
    double pitch,
    double distance,
    double targetX,
    double targetY,
    double targetZ,
    double panX,
    double panY,
    double panZ
) {}
