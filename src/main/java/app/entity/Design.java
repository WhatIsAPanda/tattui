package app.entity;
import javafx.scene.image.Image;
public record Design(String id, String name, Image image, long addedAtEpochMs) {}
