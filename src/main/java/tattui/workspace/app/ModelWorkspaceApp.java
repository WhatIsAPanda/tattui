package app;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.SceneAntialiasing;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Main JavaFX application that hosts the 3D viewer and proportion controls.
 */
public class ModelWorkspaceApp extends Application {
    private static final double TARGET_HEIGHT = 1700.0;
    private static final double MIN_DISTANCE = 300.0;
    private static final double MAX_DISTANCE = 6000.0;
    private static final double SLIDER_MIN = 0.6;
    private static final double SLIDER_MAX = 1.4;
    private static final double OVERALL_MIN = 0.8;
    private static final double OVERALL_MAX = 1.3;
    private static final double ZOOM_SENSITIVITY = 2.0;
    private static final double ORBIT_SENSITIVITY = 0.3;
    private static final double PAN_SENSITIVITY = 0.5;
    private static final double PITCH_MIN = -80.0;
    private static final double PITCH_MAX = 80.0;

    private static final List<String> BODY_PARTS = List.of(
        "Head",
        "Neck",
        "Torso",
        "Pelvis",
        "Upper Arm L",
        "Lower Arm L",
        "Hand L",
        "Upper Arm R",
        "Lower Arm R",
        "Hand R",
        "Upper Leg L",
        "Lower Leg L",
        "Foot L",
        "Upper Leg R",
        "Lower Leg R",
        "Foot R"
    );

    private static final String DEFAULT_MODEL_FILENAME = "human.obj";
    private static final String DEFAULT_MODEL_RESOURCE = "/models/human.obj";

    private static final List<String> PROPORTION_KEYS = List.of(
        "Head Size",
        "Shoulder Width",
        "Torso Width",
        "Torso Length",
        "Hip Width",
        "Arm Length",
        "Arm Thickness",
        "Leg Length",
        "Leg Thickness"
    );

    private final Map<String, Group> partGroups = new LinkedHashMap<>();
    private final Map<String, Slider> proportionControls = new LinkedHashMap<>();

    private final Group partRoot = new Group();
    private final Group overallScaleGroup = new Group(partRoot);
    private final Group modelRoot = new Group(overallScaleGroup);
    // Lighting
    private enum LightingMode { UNLIT, STUDIO }
    private LightingMode lightingMode = LightingMode.STUDIO;
    private final Group lightsGroup = new Group();

    private final Group root3D = new Group(modelRoot, lightsGroup);

    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final DoubleProperty yaw = new SimpleDoubleProperty(30.0);
    private final DoubleProperty pitch = new SimpleDoubleProperty(-20.0);
    private final DoubleProperty distance = new SimpleDoubleProperty(2000.0);
    private final DoubleProperty overallScale = new SimpleDoubleProperty(1.0);

    private ModelManipulator modelManipulator;
    private Slider overallScaleSlider;
    private Stage primaryStage;

    // Tattoo pipeline
    private Image baseTexture;
    private Canvas skinCanvas;
    private GraphicsContext gc;
    private WritableImage paintedTexture;
    private final List<Tattoo> tattoos = new ArrayList<>();
    private Tattoo selectedTattoo;
    private Image pendingTattooImage;
    private final List<PhongMaterial> tattooMaterials = new ArrayList<>();

    private Button loadTattooButton;
    private Slider tattooSizeSlider;
    private Slider tattooOpacitySlider;
    private Slider tattooRotationSlider;
    private boolean modelHasUVs;

    private Point3D cameraTarget = new Point3D(0, TARGET_HEIGHT * 0.55, 0);
    private Point3D modelCenter = Point3D.ZERO;
    private Bounds currentBounds;

    private SubScene subScene;
    private boolean adjustingSliders;

    private double lastMouseX;
    private double lastMouseY;

    @Override
    public void start(Stage stage) {
        initializePartGroups();
        configureCamera();

        this.primaryStage = stage;
        BorderPane root = new BorderPane();
        ScrollPane controlPanel = createControlPanel(stage);
        StackPane viewerPane = createViewer();

        root.setTop(createToolbar(stage));
        root.setLeft(controlPanel);
        root.setCenter(viewerPane);

        controlPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.5));
        controlPanel.maxWidthProperty().bind(root.widthProperty().multiply(0.5));
        viewerPane.prefWidthProperty().bind(root.widthProperty().multiply(0.5));

        Scene scene = new Scene(root, 1280, 800, true);
        stage.setTitle("3D Model Workspace");
        stage.setScene(scene);
        stage.show();

        loadInitialModel();
    }

    private void initializePartGroups() {
        partRoot.getChildren().clear();
        for (String part : BODY_PARTS) {
            Group group = new Group();
            group.setId(part);
            partGroups.put(part, group);
            partRoot.getChildren().add(group);
        }
        overallScaleGroup.getTransforms().clear();
        overallScaleGroup.scaleXProperty().bind(overallScale);
        overallScaleGroup.scaleYProperty().bind(overallScale);
        overallScaleGroup.scaleZProperty().bind(overallScale);
    }

    private HBox createToolbar(Stage stage) {
        Button loadButton = new Button("Load Model");
        loadButton.setOnAction(evt -> showLoadDialog(stage));

        Button resetButton = new Button("Reset View");
        resetButton.setOnAction(evt -> resetView());

        Label lightingLabel = new Label("Lighting:");
        ComboBox<LightingMode> lightingBox = new ComboBox<>(FXCollections.observableArrayList(LightingMode.values()));
        lightingBox.setValue(lightingMode);
        lightingBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV != lightingMode) {
                lightingMode = newV;
                applyLighting();
            }
        });

        HBox toolbar = new HBox(10, loadButton, resetButton, lightingLabel, lightingBox);
        toolbar.setPadding(new Insets(10));
        return toolbar;
    }

    private ScrollPane createControlPanel(Stage stage) {
        VBox controls = new VBox(12);
        controls.setPadding(new Insets(16));
        controls.setFillWidth(true);

        proportionControls.clear();
        for (String label : PROPORTION_KEYS) {
            Slider slider = createSlider(SLIDER_MIN, SLIDER_MAX);
            slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (adjustingSliders) {
                    return;
                }
                applyCurrentProportions();
            });
            proportionControls.put(label, slider);
            controls.getChildren().add(createLabeledControl(label, slider));
        }

        Slider overallSlider = createSlider(OVERALL_MIN, OVERALL_MAX);
        overallSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (adjustingSliders) {
                return;
            }
            overallScale.set(newVal.doubleValue());
        });
        overallScaleSlider = overallSlider;
        controls.getChildren().add(createLabeledControl("Overall Scale", overallSlider));

        Label tattooLabel = new Label("Tattoo Tools");
        loadTattooButton = new Button("Load Tattoo");
        loadTattooButton.setOnAction(e -> handleLoadTattoo(stage));

        tattooSizeSlider = new Slider(0.05, 1.0, 0.20);
        tattooSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedTattoo != null) {
                selectedTattoo.scale = newVal.doubleValue();
                repaintTattooTexture();
            }
        });

        tattooOpacitySlider = new Slider(0.3, 1.0, 1.0);
        tattooOpacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedTattoo != null) {
                selectedTattoo.alpha = newVal.doubleValue();
                repaintTattooTexture();
            }
        });

        tattooRotationSlider = new Slider(-180.0, 180.0, 0.0);
        tattooRotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedTattoo != null) {
                selectedTattoo.rotation = newVal.doubleValue();
                repaintTattooTexture();
            }
        });

        controls.getChildren().add(new VBox(6,
            tattooLabel,
            loadTattooButton,
            createLabeledControl("Tattoo Size", tattooSizeSlider),
            createLabeledControl("Tattoo Opacity", tattooOpacitySlider),
            createLabeledControl("Tattoo Rotation", tattooRotationSlider)
        ));

        ScrollPane scrollPane = new ScrollPane(controls);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPadding(Insets.EMPTY);
        updateTattooControlsState();
        return scrollPane;
    }

    private Slider createSlider(double min, double max) {
        Slider slider = new Slider(min, max, 1.0);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.setBlockIncrement(0.05);
        slider.setMajorTickUnit(0.1);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(false);
        slider.setMaxWidth(Double.MAX_VALUE);
        return slider;
    }

    private VBox createLabeledControl(String labelText, Slider slider) {
        Label label = new Label(labelText);
        VBox box = new VBox(4, label, slider);
        box.setFillWidth(true);
        return box;
    }

    private void handleLoadTattoo(Stage stage) {
        if (!modelHasUVs || skinCanvas == null) {
            showNoUVMessage();
            return;
        }
        Stage targetStage = stage != null ? stage : primaryStage;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Tattoo Image");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.svg"),
            new FileChooser.ExtensionFilter("PNG", "*.png"),
            new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("SVG", "*.svg")
        );
        File file = chooser.showOpenDialog(targetStage);
        if (file == null) {
            return;
        }
        Image loaded = loadTattooImage(file);
        if (loaded == null) {
            return;
        }
        pendingTattooImage = loaded;
        tattoos.clear();
        selectedTattoo = null;
        repaintTattooTexture();
    }

    private Image loadTattooImage(File file) {
        try {
            Image image = new Image(file.toURI().toString());
            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                Throwable cause = image.getException();
                throw new IOException(cause != null ? cause : new IllegalArgumentException("Unsupported image"));
            }
            return image;
        } catch (Exception ex) {
            showError("Failed to load tattoo image: " + file.getName(), ex instanceof IOException ? (IOException) ex : new IOException(ex));
            return null;
        }
    }

    private StackPane createViewer() {
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: #eef3f9;");

        subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#eef3f9"));
        subScene.setCamera(camera);
        subScene.setFocusTraversable(true);

        installInteractionHandlers();
        installTattooPlacementHandlers();
        applyLighting();

        container.getChildren().add(subScene);
        StackPane.setMargin(subScene, Insets.EMPTY);

        subScene.widthProperty().bind(container.widthProperty());
        subScene.heightProperty().bind(container.heightProperty());

        return container;
    }

    private void configureCamera() {
        camera.setNearClip(0.1);
        camera.setFarClip(20000);
        camera.setFieldOfView(35);

        yaw.addListener((obs, oldV, newV) -> updateCameraTransform());
        pitch.addListener((obs, oldV, newV) -> updateCameraTransform());
        distance.addListener((obs, oldV, newV) -> updateCameraTransform());

        updateCameraTransform();
    }

    private void applyLighting() {
        lightsGroup.getChildren().clear();

        switch (lightingMode) {
            case UNLIT -> {
                AmbientLight ambient = new AmbientLight(Color.WHITE);
                lightsGroup.getChildren().add(ambient);
                applySpecularToModel(Color.BLACK);
            }
            case STUDIO -> {
                AmbientLight ambient = new AmbientLight(Color.color(0.60, 0.60, 0.65));

                PointLight key = new PointLight(Color.WHITE);
                key.setTranslateX(-600);
                key.setTranslateY(-800);
                key.setTranslateZ(-1200);

                PointLight fill = new PointLight(Color.color(0.85, 0.90, 1.00));
                fill.setTranslateX(800);
                fill.setTranslateY(-400);
                fill.setTranslateZ(-500);

                lightsGroup.getChildren().addAll(ambient, key, fill);
                applySpecularToModel(Color.gray(0.20));
            }
        }
    }

    private void applySpecularToModel(Color specular) {
        applySpecularRecursive(modelRoot, specular);
    }

    private void applySpecularRecursive(Node n, Color specular) {
        if (n instanceof Shape3D s && s.getMaterial() instanceof PhongMaterial pm) {
            pm.setSpecularColor(specular);
        }
        if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                applySpecularRecursive(c, specular);
            }
        }
    }

    // OPTIONAL: call once after model load if you suspect bad/inside-out normals
    private void disableBackFaceCullingForAllMeshes() {
        traverse(modelRoot, n -> {
            if (n instanceof Shape3D s) {
                s.setCullFace(CullFace.NONE);
            }
        });
    }

    private void traverse(Node n, Consumer<Node> visitor) {
        visitor.accept(n);
        if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                traverse(c, visitor);
            }
        }
    }

    private void installInteractionHandlers() {
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {
                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
            }
        });

        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
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
        });

        subScene.addEventHandler(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) {
                return;
            }
            double delta = event.getDeltaY() * ZOOM_SENSITIVITY;
            distance.set(clamp(distance.get() - delta, MIN_DISTANCE, MAX_DISTANCE));
        });

        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.MIDDLE || (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)) {
                resetView();
            }
        });

        subScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                resetView();
            }
        });
    }

    private void installTattooPlacementHandlers() {
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleTattooPress);
        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleTattooDrag);
    }

    private void orbit(double dx, double dy) {
        yaw.set(normalizeAngle(yaw.get() - dx * ORBIT_SENSITIVITY));
        pitch.set(clamp(pitch.get() - dy * ORBIT_SENSITIVITY, PITCH_MIN, PITCH_MAX));
    }

    private void pan(double dx, double dy) {
        Bounds bounds = currentBounds;
        if (bounds == null) {
            return;
        }
        double factor = distance.get() / 800.0;
        double newX = cameraTarget.getX() - dx * PAN_SENSITIVITY * factor;
        double newY = cameraTarget.getY() + dy * PAN_SENSITIVITY * factor;
        double newZ = cameraTarget.getZ() + dx * PAN_SENSITIVITY * factor * 0.6;

        cameraTarget = clampTarget(new Point3D(newX, newY, newZ), bounds);
        updateCameraTransform();
    }

    private void updateCameraTransform() {
        double clampedDistance = clamp(distance.get(), MIN_DISTANCE, MAX_DISTANCE);
        if (clampedDistance != distance.get()) {
            distance.set(clampedDistance);
        }

        camera.getTransforms().setAll(
            new Translate(cameraTarget.getX(), cameraTarget.getY(), cameraTarget.getZ()),
            new Rotate(yaw.get(), Rotate.Y_AXIS),
            new Rotate(pitch.get(), Rotate.X_AXIS),
            new Translate(0, 0, -clampedDistance)
        );
    }

    private void resetView() {
        updateCurrentBounds();
        if (currentBounds == null) {
            return;
        }

        double centerX = (currentBounds.getMinX() + currentBounds.getMaxX()) * 0.5;
        double centerZ = (currentBounds.getMinZ() + currentBounds.getMaxZ()) * 0.5;
        double floor = currentBounds.getMinY();
        double height = currentBounds.getHeight();
        double targetY = floor + height * 0.55;

        modelCenter = new Point3D(centerX, targetY, centerZ);
        cameraTarget = modelCenter;
        yaw.set(30.0);
        pitch.set(-20.0);

        double maxDim = Math.max(currentBounds.getWidth(), Math.max(currentBounds.getHeight(), currentBounds.getDepth()));
        double fitDistance = maxDim * 1.2 + 350.0;
        distance.set(clamp(fitDistance, MIN_DISTANCE, MAX_DISTANCE));

        updateCameraTransform();
    }

    private void showLoadDialog(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select OBJ Model");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wavefront OBJ", "*.obj"));
        Path initial = Paths.get(System.getProperty("user.dir"), "HumanModel.obj");
        if (Files.exists(initial)) {
            chooser.setInitialDirectory(initial.getParent().toFile());
        }

        java.io.File file = chooser.showOpenDialog(stage);
        if (file != null) {
            loadFromPath(file.toPath());
        }
    }

    private void loadInitialModel() {
        Path workingDirModel = Paths.get(System.getProperty("user.dir")).resolve(DEFAULT_MODEL_FILENAME);
        if (Files.exists(workingDirModel)) {
            loadFromPath(workingDirModel);
            return;
        }

        URL resource = getClass().getResource(DEFAULT_MODEL_RESOURCE);
        if (resource != null) {
            try (InputStream stream = resource.openStream()) {
                Path temp = Files.createTempFile("human-model", ".obj");
                temp.toFile().deleteOnExit();
                Files.copy(stream, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                loadFromPath(temp);
                return;
            } catch (IOException ex) {
                showError("Unable to load bundled model", ex);
            }
        }

        applyModel(createPlaceholderModel());
    }

    private void loadFromPath(Path path) {
        try {
            ObjLoader.LoadedModel model = ObjLoader.load(path);
            applyModel(model);
        } catch (IOException ex) {
            showError("Failed to load OBJ model: " + path.getFileName(), ex);
            applyModel(createPlaceholderModel());
        }
    }

    private void applyModel(ObjLoader.LoadedModel loadedModel) {
        adjustingSliders = true;
        modelManipulator = null;
        try {
            partRoot.getTransforms().clear();
            for (Group group : partGroups.values()) {
                group.getChildren().clear();
                group.setScaleX(1.0);
                group.setScaleY(1.0);
                group.setScaleZ(1.0);
            }

            for (Slider slider : proportionControls.values()) {
                slider.setValue(1.0);
            }
            if (overallScaleSlider != null) {
                overallScaleSlider.setValue(1.0);
            }
            overallScale.set(1.0);

            for (ObjLoader.ModelPart part : loadedModel.parts()) {
                Group targetGroup = resolvePartGroup(part.name());
                targetGroup.getChildren().add(part.node());
            }

            if (loadedModel.requiresYAxisFlip()) {
                partRoot.getTransforms().add(new Scale(1, -1, 1));
            }

            if (loadedModel.requiresZUpCorrection()) {
                partRoot.getTransforms().add(new Rotate(-90, Rotate.X_AXIS));
            }

            normalizeModel();
            initModelManipulatorFromScene();
            if (!modelHasUVs) {
                showNoUVMessage();
            }
            initializeTattooPipeline();
            updateCurrentBounds();
            resetView();
            applyLighting();
            applyCurrentProportions();
        } finally {
            adjustingSliders = false;
        }
    }

    private void normalizeModel() {
        if (partRoot.getChildren().isEmpty()) {
            return;
        }

        Bounds initialBounds = partRoot.getBoundsInParent();
        if (initialBounds.getHeight() == 0) {
            return;
        }

        Scale scale = new Scale();
        double scaleFactor = TARGET_HEIGHT / initialBounds.getHeight();
        scale.setX(scaleFactor);
        scale.setY(scaleFactor);
        scale.setZ(scaleFactor);
        partRoot.getTransforms().add(scale);

        Bounds scaledBounds = partRoot.getBoundsInParent();
        double translateX = -scaledBounds.getMinX() - scaledBounds.getWidth() / 2.0;
        double translateZ = -scaledBounds.getMinZ() - scaledBounds.getDepth() / 2.0;
        double translateY = -scaledBounds.getMinY();

        partRoot.getTransforms().add(new Translate(translateX, translateY, translateZ));
    }

    private void initModelManipulatorFromScene() {
        List<TriangleMesh> meshes = new ArrayList<>();
        collectMeshes(modelRoot, meshes);
        modelHasUVs = meshes.stream().anyMatch(mesh -> mesh.getTexCoords().size() > 0);
        modelManipulator = meshes.isEmpty() ? null : new ModelManipulator(meshes);
    }

    private void collectMeshes(Node node, List<TriangleMesh> out) {
        if (node instanceof MeshView mv && mv.getMesh() instanceof TriangleMesh tm) {
            out.add(tm);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectMeshes(child, out);
            }
        }
    }

    private void initializeTattooPipeline() {
        tattooMaterials.clear();
        collectMaterials(modelRoot, tattooMaterials);
        tattoos.clear();
        selectedTattoo = null;

        if (!modelHasUVs || tattooMaterials.isEmpty()) {
            baseTexture = null;
            skinCanvas = null;
            gc = null;
            paintedTexture = null;
            updateTattooControlsState();
            return;
        }

        Image diffuse = null;
        for (PhongMaterial pm : tattooMaterials) {
            if (pm.getDiffuseMap() != null) {
                diffuse = pm.getDiffuseMap();
                break;
            }
        }

        if (diffuse == null) {
            int width = 2048;
            int height = 2048;
            WritableImage neutral = new WritableImage(width, height);
            PixelWriter writer = neutral.getPixelWriter();
            Color fill = Color.rgb(225, 200, 180);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    writer.setColor(x, y, fill);
                }
            }
            baseTexture = neutral;
        } else {
            baseTexture = diffuse;
        }

        double texWidth = Math.max(1.0, baseTexture.getWidth());
        double texHeight = Math.max(1.0, baseTexture.getHeight());
        skinCanvas = new Canvas(texWidth, texHeight);
        gc = skinCanvas.getGraphicsContext2D();
        paintedTexture = new WritableImage((int) Math.max(1, Math.round(texWidth)), (int) Math.max(1, Math.round(texHeight)));

        repaintTattooTexture();

        for (PhongMaterial pm : tattooMaterials) {
            pm.setDiffuseMap(paintedTexture);
            pm.setDiffuseColor(Color.WHITE);
        }

        updateTattooControlsState();
    }

    private void collectMaterials(Node node, List<PhongMaterial> out) {
        if (node instanceof Shape3D shape && shape.getMaterial() instanceof PhongMaterial pm) {
            if (!out.contains(pm)) {
                out.add(pm);
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectMaterials(child, out);
            }
        }
    }

    private void repaintTattooTexture() {
        if (gc == null || skinCanvas == null || baseTexture == null) {
            return;
        }

        double width = skinCanvas.getWidth();
        double height = skinCanvas.getHeight();

        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        gc.setGlobalAlpha(1.0);
        gc.clearRect(0, 0, width, height);
        gc.drawImage(baseTexture, 0, 0, width, height);

        for (Tattoo tattoo : tattoos) {
            if (tattoo.image == null) {
                continue;
            }
            double imgWidth = tattoo.image.getWidth();
            double imgHeight = tattoo.image.getHeight();
            double px = tattoo.u * width;
            double py = tattoo.v * height;
            double drawW = imgWidth * tattoo.scale;
            double drawH = imgHeight * tattoo.scale;

            gc.save();
            gc.translate(px, py);
            gc.rotate(tattoo.rotation);
            gc.setGlobalAlpha(tattoo.alpha);
            gc.setGlobalBlendMode(BlendMode.SRC_OVER);
            gc.drawImage(tattoo.image, -drawW / 2.0, -drawH / 2.0, drawW, drawH);
            gc.restore();
        }

        skinCanvas.snapshot(null, paintedTexture);

        for (PhongMaterial pm : tattooMaterials) {
            pm.setDiffuseMap(paintedTexture);
        }
    }

    private Map<String, Double> gatherCurrentProportions() {
        LinkedHashMap<String, Double> values = new LinkedHashMap<>();
        proportionControls.forEach((key, slider) -> {
            double rounded = Math.round(slider.getValue() * 1000.0) / 1000.0;
            values.put(key, rounded);
        });
        return values;
    }

    private void applyCurrentProportions() {
        if (modelManipulator == null) {
            return;
        }
        modelManipulator.apply(gatherCurrentProportions());
        updateCurrentBounds();
    }

    private void updateTattooControlsState() {
        boolean enabled = modelHasUVs && skinCanvas != null;
        if (loadTattooButton != null) {
            loadTattooButton.setDisable(!enabled);
        }
        if (tattooSizeSlider != null) {
            tattooSizeSlider.setDisable(!enabled);
        }
        if (tattooOpacitySlider != null) {
            tattooOpacitySlider.setDisable(!enabled);
        }
        if (tattooRotationSlider != null) {
            tattooRotationSlider.setDisable(!enabled);
        }
    }

    private void handleTattooPress(MouseEvent event) {
        if (!modelHasUVs || skinCanvas == null) {
            return;
        }
        if (event.getButton() == MouseButton.SECONDARY) {
            tattoos.clear();
            selectedTattoo = null;
            repaintTattooTexture();
            return;
        }
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (selectedTattoo == null && pendingTattooImage == null) {
            return;
        }
        PickResult result = event.getPickResult();
        if (result == null || !(result.getIntersectedNode() instanceof MeshView)) {
            return;
        }
        Point2D uv = result.getIntersectedTexCoord();
        if (uv == null) {
            return;
        }

        double u = clamp(uv.getX(), 0.0, 1.0);
        double v = clamp(uv.getY(), 0.0, 1.0);
        double size = tattooSizeSlider != null ? tattooSizeSlider.getValue() : 0.2;
        double opacity = tattooOpacitySlider != null ? tattooOpacitySlider.getValue() : 1.0;
        double rotation = tattooRotationSlider != null ? tattooRotationSlider.getValue() : 0.0;

        if (selectedTattoo == null) {
            selectedTattoo = new Tattoo(u, v, pendingTattooImage, size, rotation, opacity);
            tattoos.clear();
            tattoos.add(selectedTattoo);
            pendingTattooImage = null;
        } else {
            selectedTattoo.u = u;
            selectedTattoo.v = v;
            if (pendingTattooImage != null) {
                selectedTattoo.image = pendingTattooImage;
                selectedTattoo.scale = size;
                selectedTattoo.rotation = rotation;
                selectedTattoo.alpha = opacity;
                pendingTattooImage = null;
            }
        }

        tattoos.clear();
        tattoos.add(selectedTattoo);
        repaintTattooTexture();
        event.consume();
    }

    private void handleTattooDrag(MouseEvent event) {
        if (selectedTattoo == null || !modelHasUVs || skinCanvas == null || !event.isPrimaryButtonDown()) {
            return;
        }
        PickResult result = event.getPickResult();
        if (result == null || !(result.getIntersectedNode() instanceof MeshView)) {
            return;
        }
        Point2D uv = result.getIntersectedTexCoord();
        if (uv == null) {
            return;
        }
        selectedTattoo.u = clamp(uv.getX(), 0.0, 1.0);
        selectedTattoo.v = clamp(uv.getY(), 0.0, 1.0);
        repaintTattooTexture();
        event.consume();
    }

    private void updateCurrentBounds() {
        currentBounds = overallScaleGroup.getBoundsInParent();
    }

    private ObjLoader.LoadedModel createPlaceholderModel() {
        List<ObjLoader.ModelPart> parts = new ArrayList<>();
        PhongMaterial neutral = new PhongMaterial(Color.BEIGE);
        PhongMaterial limbs = new PhongMaterial(Color.LIGHTGRAY);

        Sphere head = new Sphere(120);
        head.setMaterial(neutral);
        head.setTranslateY(150);
        parts.add(new ObjLoader.ModelPart("Head", head));

        Cylinder neck = new Cylinder(55, 120);
        neck.setMaterial(neutral);
        neck.setTranslateY(60);
        parts.add(new ObjLoader.ModelPart("Neck", neck));

        Cylinder torso = new Cylinder(160, 500);
        torso.setMaterial(neutral);
        torso.setTranslateY(-220);
        parts.add(new ObjLoader.ModelPart("Torso", torso));

        Cylinder pelvis = new Cylinder(150, 160);
        pelvis.setMaterial(neutral);
        pelvis.setTranslateY(-440);
        parts.add(new ObjLoader.ModelPart("Pelvis", pelvis));

        parts.add(new ObjLoader.ModelPart("Upper Arm L", createLimbSegment(-220, -120, 320, limbs)));
        parts.add(new ObjLoader.ModelPart("Lower Arm L", createLimbSegment(-220, -120, 520, limbs)));
        parts.add(new ObjLoader.ModelPart("Hand L", createHand(-220, -120, 680, limbs)));

        parts.add(new ObjLoader.ModelPart("Upper Arm R", createLimbSegment(220, -120, 320, limbs)));
        parts.add(new ObjLoader.ModelPart("Lower Arm R", createLimbSegment(220, -120, 520, limbs)));
        parts.add(new ObjLoader.ModelPart("Hand R", createHand(220, -120, 680, limbs)));

        parts.add(new ObjLoader.ModelPart("Upper Leg L", createLegSegment(-120, -540, -250, limbs)));
        parts.add(new ObjLoader.ModelPart("Lower Leg L", createLegSegment(-120, -840, -450, limbs)));
        parts.add(new ObjLoader.ModelPart("Foot L", createFoot(-120, -980, -550, limbs)));

        parts.add(new ObjLoader.ModelPart("Upper Leg R", createLegSegment(120, -540, -250, limbs)));
        parts.add(new ObjLoader.ModelPart("Lower Leg R", createLegSegment(120, -840, -450, limbs)));
        parts.add(new ObjLoader.ModelPart("Foot R", createFoot(120, -980, -550, limbs)));

        return new ObjLoader.LoadedModel(parts, false, false);
    }

    private Node createLimbSegment(double x, double y, double z, PhongMaterial material) {
        Cylinder segment = new Cylinder(55, 260);
        segment.setMaterial(material);
        segment.setTranslateX(x);
        segment.setTranslateY(y);
        segment.setTranslateZ(z);
        return segment;
    }

    private Node createLegSegment(double x, double y, double z, PhongMaterial material) {
        Cylinder segment = new Cylinder(70, 320);
        segment.setMaterial(material);
        segment.setTranslateX(x);
        segment.setTranslateY(y);
        segment.setTranslateZ(z);
        return segment;
    }

    private Node createHand(double x, double y, double z, PhongMaterial material) {
        Sphere hand = new Sphere(60);
        hand.setMaterial(material);
        hand.setTranslateX(x);
        hand.setTranslateY(y);
        hand.setTranslateZ(z);
        return hand;
    }

    private Node createFoot(double x, double y, double z, PhongMaterial material) {
        Box foot = new Box(120, 50, 200);
        foot.setMaterial(material);
        foot.setTranslateX(x);
        foot.setTranslateY(y);
        foot.setTranslateZ(z + 100);
        return foot;
    }

    private Group resolvePartGroup(String originalName) {
        if (originalName != null) {
            String normalized = originalName.trim().toLowerCase(Locale.ROOT);
            for (String part : BODY_PARTS) {
                if (normalized.contains(part.toLowerCase(Locale.ROOT))) {
                    return Objects.requireNonNull(partGroups.get(part));
                }
            }
        }
        return Objects.requireNonNull(partGroups.get("Torso"));
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
        return Math.max(min, Math.min(max, value));
    }

    private double normalizeAngle(double angle) {
        double mod = angle % 360.0;
        return mod < 0 ? mod + 360.0 : mod;
    }

    private void showError(String message, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(ex.getMessage());
        alert.show();
    }

    private void showNoUVMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Tattoo placement unavailable");
        alert.setContentText("This model has no UVs; tattoo placement requires UVs.");
        alert.show();
    }

    private static final class Tattoo {
        double u;
        double v;
        double scale;
        double rotation;
        double alpha;
        Image image;

        Tattoo(double u, double v, Image image, double scale, double rotation, double alpha) {
            this.u = u;
            this.v = v;
            this.image = image;
            this.scale = scale;
            this.rotation = rotation;
            this.alpha = alpha;
        }
    }

    private static final class ModelManipulator {
        private final List<TriangleMesh> meshes;
        private final List<float[]> originalPoints;
        private final List<float[]> workingBuffers;

        private final float minX;
        private final float maxX;
        private final float minY;
        private final float maxY;
        private final float minZ;
        private final float maxZ;
        private final float centerX;
        private final float centerZ;

        private final float headThreshold;
        private final float shoulderLevel;
        private final float torsoBottom;
        private final float hipLevel;
        private final float kneeLevel;

        private final float torsoSpan;
        private final float armSpan;
        private final float legSpan;
        private final float armThreshold;

        private final float headCenterX;
        private final float headCenterZ;
        private final float[] leftArmCenter = new float[3];
        private final float[] rightArmCenter = new float[3];
        private final float[] leftLegCenter = new float[3];
        private final float[] rightLegCenter = new float[3];

        private final float armMinY;
        private final float legMinY;
        private final float shoulderRadius;
        private final float hipRadius;
        private final boolean hasHead;

        ModelManipulator(List<TriangleMesh> meshes) {
            this.meshes = new ArrayList<>(meshes);
            this.originalPoints = new ArrayList<>(meshes.size());
            this.workingBuffers = new ArrayList<>(meshes.size());

            float minX = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;

            List<Float> ySamples = new ArrayList<>();

            for (TriangleMesh mesh : meshes) {
                float[] source = new float[mesh.getPoints().size()];
                mesh.getPoints().toArray(source);
                originalPoints.add(source);
                workingBuffers.add(new float[source.length]);

                for (int i = 0; i < source.length; i += 3) {
                    float x = source[i];
                    float y = source[i + 1];
                    float z = source[i + 2];

                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                    ySamples.add(y);
                }
            }

            if (ySamples.isEmpty()) {
                minX = maxX = minY = maxY = minZ = maxZ = 0f;
            }

            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.centerX = (minX + maxX) * 0.5f;
            this.centerZ = (minZ + maxZ) * 0.5f;

            float[] sortedY = new float[ySamples.size()];
            for (int i = 0; i < ySamples.size(); i++) {
                sortedY[i] = ySamples.get(i);
            }
            Arrays.sort(sortedY);

            this.headThreshold = quantile(sortedY, 0.88f);
            this.shoulderLevel = quantile(sortedY, 0.79f);
            this.torsoBottom = quantile(sortedY, 0.42f);
            this.hipLevel = quantile(sortedY, 0.35f);
            this.kneeLevel = quantile(sortedY, 0.20f);

            float height = Math.max(maxY - minY, 1f);
            this.shoulderRadius = Math.max(height * 0.04f, 1f);
            this.hipRadius = Math.max(height * 0.05f, 1f);
            this.armThreshold = Math.max((maxX - minX) * 0.35f, 1f);

            float headSumX = 0f;
            float headSumZ = 0f;
            int headCount = 0;

            float leftArmSumX = 0f;
            float leftArmSumY = 0f;
            float leftArmSumZ = 0f;
            int leftArmCount = 0;

            float rightArmSumX = 0f;
            float rightArmSumY = 0f;
            float rightArmSumZ = 0f;
            int rightArmCount = 0;

            float leftLegSumX = 0f;
            float leftLegSumY = 0f;
            float leftLegSumZ = 0f;
            int leftLegCount = 0;

            float rightLegSumX = 0f;
            float rightLegSumY = 0f;
            float rightLegSumZ = 0f;
            int rightLegCount = 0;

            float armMinY = Float.POSITIVE_INFINITY;
            float legMinY = Float.POSITIVE_INFINITY;

            for (float[] source : originalPoints) {
                for (int i = 0; i < source.length; i += 3) {
                    float x = source[i];
                    float y = source[i + 1];
                    float z = source[i + 2];

                    if (y >= headThreshold) {
                        headSumX += x;
                        headSumZ += z;
                        headCount++;
                    }

                    boolean isArm = Math.abs(x - centerX) > armThreshold && y > kneeLevel;
                    if (isArm) {
                        if (x < centerX) {
                            leftArmSumX += x;
                            leftArmSumY += y;
                            leftArmSumZ += z;
                            leftArmCount++;
                        } else {
                            rightArmSumX += x;
                            rightArmSumY += y;
                            rightArmSumZ += z;
                            rightArmCount++;
                        }
                        armMinY = Math.min(armMinY, y);
                    }

                    if (y <= hipLevel) {
                        if (x < centerX) {
                            leftLegSumX += x;
                            leftLegSumY += y;
                            leftLegSumZ += z;
                            leftLegCount++;
                        } else {
                            rightLegSumX += x;
                            rightLegSumY += y;
                            rightLegSumZ += z;
                            rightLegCount++;
                        }
                        legMinY = Math.min(legMinY, y);
                    }
                }
            }

            this.hasHead = headCount > 0;
            this.headCenterX = hasHead ? headSumX / headCount : centerX;
            this.headCenterZ = hasHead ? headSumZ / headCount : centerZ;

            leftArmCenter[0] = leftArmCount > 0 ? leftArmSumX / leftArmCount : centerX - armThreshold;
            leftArmCenter[1] = leftArmCount > 0 ? leftArmSumY / leftArmCount : shoulderLevel;
            leftArmCenter[2] = leftArmCount > 0 ? leftArmSumZ / leftArmCount : centerZ;

            rightArmCenter[0] = rightArmCount > 0 ? rightArmSumX / rightArmCount : centerX + armThreshold;
            rightArmCenter[1] = rightArmCount > 0 ? rightArmSumY / rightArmCount : shoulderLevel;
            rightArmCenter[2] = rightArmCount > 0 ? rightArmSumZ / rightArmCount : centerZ;

            float hipOffset = Math.max((maxX - minX) * 0.2f, 1f);
            leftLegCenter[0] = leftLegCount > 0 ? leftLegSumX / leftLegCount : centerX - hipOffset;
            leftLegCenter[1] = leftLegCount > 0 ? leftLegSumY / leftLegCount : hipLevel - (height * 0.25f);
            leftLegCenter[2] = leftLegCount > 0 ? leftLegSumZ / leftLegCount : centerZ;

            rightLegCenter[0] = rightLegCount > 0 ? rightLegSumX / rightLegCount : centerX + hipOffset;
            rightLegCenter[1] = rightLegCount > 0 ? rightLegSumY / rightLegCount : hipLevel - (height * 0.25f);
            rightLegCenter[2] = rightLegCount > 0 ? rightLegSumZ / rightLegCount : centerZ;

            if (Float.isInfinite(armMinY)) {
                armMinY = Math.min(shoulderLevel, kneeLevel + height * 0.1f);
            }
            if (armMinY >= shoulderLevel) {
                armMinY = shoulderLevel - Math.max(height * 0.05f, 1f);
            }
            if (Float.isInfinite(legMinY)) {
                legMinY = minY;
            }

            this.armMinY = armMinY;
            this.legMinY = legMinY;
            this.torsoSpan = Math.max(shoulderLevel - torsoBottom, 1f);
            this.armSpan = Math.max(shoulderLevel - armMinY, 1f);
            this.legSpan = Math.max(hipLevel - legMinY, 1f);
        }

        void apply(Map<String, Double> params) {
            float headSize = getParam(params, "Head Size");
            float shoulderWidth = getParam(params, "Shoulder Width");
            float torsoWidth = getParam(params, "Torso Width");
            float torsoLength = getParam(params, "Torso Length");
            float hipWidth = getParam(params, "Hip Width");
            float armLength = getParam(params, "Arm Length");
            float armThickness = getParam(params, "Arm Thickness");
            float legLength = getParam(params, "Leg Length");
            float legThickness = getParam(params, "Leg Thickness");

            float torsoDelta = (torsoLength - 1f) * torsoSpan;
            float legDelta = (legLength - 1f) * legSpan;
            float armDelta = (armLength - 1f) * armSpan;

            float modelHeight = Math.max(1f, maxY - minY);
            float shoulderRadius = Math.max(0.08f * modelHeight, 0.35f * torsoSpan);
            float hipRadius = Math.max(0.08f * modelHeight, 0.30f * legSpan);

            for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
                float[] original = originalPoints.get(meshIndex);
                float[] dst = workingBuffers.get(meshIndex);
                System.arraycopy(original, 0, dst, 0, original.length);

                for (int i = 0; i < original.length; i += 3) {
                    float x = original[i];
                    float y = original[i + 1];
                    float z = original[i + 2];

                    boolean isArm = Math.abs(x - centerX) > armThreshold && y > kneeLevel;
                    float ny;

                    if (isArm) {
                        if (y <= shoulderLevel) {
                            float normalized = clamp((shoulderLevel - y) / armSpan, 0f, 1f);
                            ny = shoulderLevel - armSpan * (armLength * normalized) + torsoDelta + legDelta;
                        } else {
                            ny = y + armDelta + torsoDelta + legDelta;
                        }
                    } else if (y <= hipLevel) {
                        float normalized = clamp((hipLevel - y) / legSpan, 0f, 1f);
                        ny = hipLevel - legSpan * (legLength * normalized);
                    } else if (y <= shoulderLevel) {
                        float normalized = clamp((y - torsoBottom) / torsoSpan, 0f, 1f);
                        ny = torsoBottom + torsoSpan * (torsoLength * normalized) + legDelta;
                    } else {
                        ny = y + torsoDelta + legDelta;
                    }

                    float nx = x;
                    float nz = z;

                    boolean isLeg = ny <= hipLevel;

                    if (hasHead && y >= headThreshold && headSize != 1f) {
                        float weight = smoothStep(headThreshold, maxY, y);
                        if (weight > 0f) {
                            float scale = 1f + (headSize - 1f) * weight;
                            nx = headCenterX + (nx - headCenterX) * scale;
                            nz = headCenterZ + (nz - headCenterZ) * scale;
                        }
                    }

                    if (Math.abs(shoulderWidth - 1.0) > 1e-3) {
                        float wShoulder = bandWeight(ny, shoulderLevel, shoulderRadius);
                        if (wShoulder > 0f) {
                            float armAtten = isArm ? 0.35f : 1.0f;
                            float sx = f(1.0 + (shoulderWidth - 1.0) * wShoulder * armAtten);
                            float sz = f(1.0 + (shoulderWidth - 1.0) * 0.35 * wShoulder * armAtten);
                            sx = clamp(sx, 0.4f, 2.5f);
                            sz = clamp(sz, 0.4f, 2.5f);
                            nx = centerX + (nx - centerX) * sx;
                            nz = centerZ + (nz - centerZ) * sz;
                        }
                    }

                    if (!isArm && Math.abs(torsoWidth - 1.0) > 1e-3) {
                        float wTorso = smoothStep(torsoBottom, shoulderLevel, ny);
                        if (wTorso > 0f) {
                            float sx = f(1.0 + (torsoWidth - 1.0) * wTorso);
                            float sz = f(1.0 + (torsoWidth - 1.0) * 0.5 * wTorso);
                            sx = clamp(sx, 0.4f, 2.5f);
                            sz = clamp(sz, 0.4f, 2.5f);
                            nx = centerX + (nx - centerX) * sx;
                            nz = centerZ + (nz - centerZ) * sz;
                        }
                    }

                    if (Math.abs(hipWidth - 1.0) > 1e-3) {
                        float wHip = bandWeight(ny, hipLevel, hipRadius);
                        if (wHip > 0f) {
                            float legBlendDown = 1.0f - smoothStep(legMinY, hipLevel, ny);
                            float legAtten = isLeg ? (0.5f * (1.0f - 0.7f * legBlendDown)) : 1.0f;
                            float sx = f(1.0 + (hipWidth - 1.0) * wHip * legAtten);
                            float sz = f(1.0 + (hipWidth - 1.0) * 0.45 * wHip * legAtten);
                            sx = clamp(sx, 0.4f, 2.5f);
                            sz = clamp(sz, 0.4f, 2.5f);
                            nx = centerX + (nx - centerX) * sx;
                            nz = centerZ + (nz - centerZ) * sz;
                        }
                    }

                    if (isArm && armThickness != 1f) {
                        float radialWeight = smoothStep(armMinY, shoulderLevel, ny);
                        if (radialWeight > 0f) {
                            float[] center = x < centerX ? leftArmCenter : rightArmCenter;
                            float scale = 1f + (armThickness - 1f) * radialWeight;
                            nx = center[0] + (nx - center[0]) * scale;
                            nz = center[2] + (nz - center[2]) * scale;
                        }
                    }

                    if (ny <= hipLevel && legThickness != 1f) {
                        float radialWeight = 1f - smoothStep(legMinY, hipLevel, ny);
                        if (radialWeight > 0f) {
                            float[] center = x < centerX ? leftLegCenter : rightLegCenter;
                            float scale = 1f + (legThickness - 1f) * radialWeight;
                            nx = center[0] + (nx - center[0]) * scale;
                            nz = center[2] + (nz - center[2]) * scale;
                        }
                    }

                    dst[i] = nx;
                    dst[i + 1] = ny;
                    dst[i + 2] = nz;
                }

                meshes.get(meshIndex).getPoints().set(0, dst, 0, dst.length);
            }
        }

        private float getParam(Map<String, Double> params, String key) {
            Double value = params.get(key);
            return value == null ? 1f : value.floatValue();
        }

        private float bandWeight(float y, float center, float radius) {
            float d = Math.abs(y - center);
            if (radius <= 1e-6f || d >= radius) {
                return 0f;
            }
            float t = 1f - (d / radius);
            float s = t * t * (3f - 2f * t);
            return s * s;
        }

        private float f(double v) {
            return (float) Math.max(-1e9, Math.min(1e9, v));
        }

        private float smoothStep(float edge0, float edge1, float v) {
            float span = edge1 - edge0;
            if (Math.abs(span) < 1e-5f) {
                return v >= edge1 ? 1f : 0f;
            }
            float t = clamp((v - edge0) / span, 0f, 1f);
            return t * t * (3f - 2f * t);
        }

        private float falloff(float v, float center, float radius) {
            float d = Math.abs(v - center);
            if (d >= radius) {
                return 0f;
            }
            float n = 1f - (d / radius);
            return n * n;
        }

        private float clamp(float v, float lo, float hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        private float quantile(float[] sorted, float q) {
            if (sorted.length == 0) {
                return 0f;
            }
            float cq = clamp(q, 0f, 1f);
            int index = (int) Math.floor(cq * (sorted.length - 1));
            index = Math.max(0, Math.min(sorted.length - 1, index));
            return sorted[index];
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
