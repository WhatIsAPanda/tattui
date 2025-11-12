package app.view3d;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.paint.Color;
import javafx.scene.AmbientLight;

public final class LightingSystem {

    public enum Mode {
        STUDIO("Studio"),
        UNLIT("Unlit");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final Group rig = new Group();
    private Mode current;

    public Node node() {
        return rig;
    }

    public void apply(Mode mode) {
        if (mode == null || mode == current) {
            return;
        }
        current = mode;
        rig.getChildren().clear();
        if (mode == Mode.STUDIO) {
            rig.getChildren().addAll(uniformRig());
        }
    }

    private Node[] uniformRig() {
        AmbientLight ambient = new AmbientLight(Color.color(1, 1, 1, 0.35));
        PointLight key   = mkLight(+300, +300, +300, 1.0);
        PointLight fill1 = mkLight(-300, +150, +300, 0.8);
        PointLight fill2 = mkLight(+300, +150, -300, 0.8);
        PointLight rim   = mkLight(-300, +250, -300, 0.7);
        return new Node[]{ ambient, key, fill1, fill2, rim };
    }

    private static PointLight mkLight(double x, double y, double z, double intensity) {
        PointLight light = new PointLight(Color.color(1, 1, 1, intensity));
        light.setTranslateX(x);
        light.setTranslateY(y);
        light.setTranslateZ(z);
        return light;
    }
}
