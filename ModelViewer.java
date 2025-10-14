import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ModelViewer extends Application {

    private final Group world = new Group();
    private final Group cameraPivot = new Group();
    private final Group sceneContent = new Group();

    // Sidebar container (nav bar target)
    private final VBox sidebarContainer = new VBox();

    private PerspectiveCamera camera;
    private double camYaw = 30, camPitch = -15, camDist = 1200;
    private double dragX, dragY, startYaw, startPitch;

    @Override
    public void start(Stage stage) throws Exception {
        world.getChildren().setAll(buildGrid(3000, 100), cameraPivot, sceneContent);

        SubScene sub = new SubScene(world, 1200, 800, true, SceneAntialiasing.BALANCED);
        sub.setFill(Color.web("#eef3f9"));
        setupCamera(sub);
        enableOrbitControls(sub);

        HBox topBar = new HBox(8);
        topBar.setPadding(new Insets(8));
        Button btnLoadModel = new Button("Load Model");
        Button btnLoadImage = new Button("Load Image");
        btnLoadModel.setOnAction(e -> {});
        btnLoadImage.setOnAction(e -> {});
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label title = new Label("3D Workspace");
        title.setStyle("-fx-font-weight: bold;");
        topBar.getChildren().addAll(btnLoadModel, btnLoadImage, spacer, title);

        VBox rightPanel = buildRightPanel();
        rightPanel.setPrefWidth(300);

        // Sidebar (nav bar) placeholder container
        sidebarContainer.setPrefWidth(240);
        sidebarContainer.setMinWidth(200);
        sidebarContainer.setStyle("-fx-background-color:#f4f6f8; -fx-border-color:#d9e2ec; -fx-border-width:0 1 0 0;");

        // Try to load the provided FXML nav bar named "taskbar.fxml"
        // 1) from working directory, 2) from classpath (resources), else show a simple placeholder.
        loadSidebarFromFXML("taskbar.fxml");

        BorderPane root = new BorderPane();
        StackPane center = new StackPane(sub);
        sub.widthProperty().bind(center.widthProperty());
        sub.heightProperty().bind(center.heightProperty());
        root.setTop(topBar);
        root.setLeft(sidebarContainer);   // FXML nav bar injected here
        root.setRight(rightPanel);
        root.setCenter(center);

        Scene scene = new Scene(root, 1280, 860, true);
        stage.setTitle("Model Viewer");
        stage.setScene(scene);
        stage.show();

        resetCameraDefault();
    }

    /** Replace left sidebar with loaded node */
    public void setSidebar(Node sidebar) {
        sidebarContainer.getChildren().setAll(sidebar);
    }

    /** Attempts to load "taskbar.fxml" as the nav bar. Falls back to a simple placeholder on failure. */
    private void loadSidebarFromFXML(String fxmlName) {
        Parent loaded = null;

        // Try file in current working directory
        File f = new File(fxmlName);
        if (f.exists() && f.isFile()) {
            try (InputStream is = new FileInputStream(f)) {
                loaded = FXMLLoader.load(new javafx.util.Pair<>(f.toURI().toURL(), is).getKey());
                // The above trick isn't necessary; simpler:
                loaded = FXMLLoader.load(f.toURI().toURL());
            } catch (Exception ignored) { loaded = null; }
        }

        // Try classpath resource (e.g., if bundled in JAR/resources)
        if (loaded == null) {
            try {
                var url = getClass().getResource("/" + fxmlName);
                if (url != null) {
                    loaded = FXMLLoader.load(url);
                }
            } catch (Exception ignored) { loaded = null; }
        }

        if (loaded != null) {
            setSidebar(loaded);
        } else {
            // Fallback placeholder nav bar
            VBox placeholder = new VBox(10);
            placeholder.setPadding(new Insets(12));
            placeholder.setStyle("-fx-background-color:#f4f6f8;");
            Label hdr = new Label("Navigation");
            hdr.setStyle("-fx-font-weight:bold; -fx-font-size:14px;");
            Button b1 = new Button("Home");
            b1.setMaxWidth(Double.MAX_VALUE);
            Button b2 = new Button("Library");
            b2.setMaxWidth(Double.MAX_VALUE);
            Button b3 = new Button("Settings");
            b3.setMaxWidth(Double.MAX_VALUE);
            placeholder.getChildren().addAll(hdr, new Separator(), b1, b2, b3);
            setSidebar(placeholder);
        }
    }

    private void setupCamera(SubScene sub) {
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        cameraPivot.getChildren().add(camera);
        sub.setCamera(camera);
        updateCamera();
    }

    private void updateCamera() {
        camera.getTransforms().setAll(
                new Rotate(camYaw, Rotate.Y_AXIS),
                new Rotate(camPitch, Rotate.X_AXIS),
                new Translate(0, 0, -camDist)
        );
        cameraPivot.setTranslateY(-400);
    }

    private void enableOrbitControls(SubScene sub) {
        sub.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragX = e.getSceneX();
                dragY = e.getSceneY();
                startYaw = camYaw;
                startPitch = camPitch;
            }
        });
        sub.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double dx = e.getSceneX() - dragX;
                double dy = e.getSceneY() - dragY;
                camYaw = startYaw + dx * 0.4;
                camPitch = clamp(startPitch - dy * 0.35, -80, 80);
                updateCamera();
            }
        });
        sub.addEventHandler(ScrollEvent.SCROLL, e -> {
            camDist = clamp(camDist + e.getDeltaY(), 300, 4000);
            updateCamera();
        });
    }

    private void resetCameraDefault() {
        camDist = 1200;
        camYaw = 30;
        camPitch = -15;
        updateCamera();
    }

    private VBox buildRightPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color:#f7fafc; -fx-border-color:#d9e2ec;");

        Label title = new Label("Proportions");
        title.setStyle("-fx-font-weight:bold; -fx-font-size:14px;");

        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(6);

        Slider sHead  = labeledSlider("Head",  0.5, 1.8, 1.0, gp, 0);
        Slider sTorso = labeledSlider("Torso", 0.5, 1.8, 1.0, gp, 1);
        Slider sArms  = labeledSlider("Arms",  0.5, 1.8, 1.0, gp, 2);
        Slider sLegs  = labeledSlider("Legs",  0.5, 1.8, 1.0, gp, 3);

        box.getChildren().addAll(title, gp, new Separator(),
                new Label("Controls:\n• Left-drag: Orbit\n• Wheel: Zoom"));
        return box;
    }

    private Slider labeledSlider(String name, double min, double max, double val, GridPane gp, int row) {
        Slider s = new Slider(min, max, val);
        s.setShowTickMarks(true);
        s.setShowTickLabels(true);
        s.setMajorTickUnit(0.5);
        gp.add(new Label(name), 0, row);
        gp.add(s, 1, row);
        return s;
    }

    private Group buildGrid(int size, int step) {
        Group g = new Group();
        int half = size / 2;
        PhongMaterial m = new PhongMaterial(Color.web("#d0d7de"));

        for (int i = -half; i <= half; i += step) {
            Box x = new Box(size, 0.5, 0.5); x.setMaterial(m); x.setTranslateZ(i);
            Box z = new Box(0.5, 0.5, size); z.setMaterial(m); z.setTranslateX(i);
            g.getChildren().addAll(x, z);
        }

        Cylinder axis = new Cylinder(2, size * 0.4);
        axis.setMaterial(new PhongMaterial(Color.web("#8aa3ff")));
        axis.setTranslateY(-axis.getHeight()/2.0);
        g.getChildren().add(axis);

        return g;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
