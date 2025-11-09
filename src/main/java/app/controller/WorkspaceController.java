package app.controller;

import app.controller.workspace.TattooWorkspace;
import app.controller.workspace.WorkspaceCamera;
import app.entity.Tattoo;
import app.loader.ObjLoader;
import app.model.ModelManipulator;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.fxml.FXML;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Application controller that encapsulates the workspace behaviour and state.
 */
public final class WorkspaceController {
    private static final String PNG_PATTERN = "*.png";
    private static final double TARGET_HEIGHT = 1700.0;
    private static final double MIN_DISTANCE = 300.0;
    private static final double MAX_DISTANCE = 6000.0;
    private static final double SLIDER_MIN = 0.6;
    private static final double SLIDER_MAX = 1.4;
    private static final double OVERALL_MIN = 0.8;
    private static final double OVERALL_MAX = 1.3;
    private static final double ORBIT_SENSITIVITY = 0.3;
    private static final double PAN_SENSITIVITY = 0.5;
    private static final double PITCH_MIN = -80.0;
    private static final double PITCH_MAX = 80.0;
    private static final int MAX_TATTOO_HISTORY = 12;
    public static final String TORSO = "Torso";
    public static final String WP_SLIDER = "workspace-slider";

    private static final List<String> BODY_PARTS = List.of(
        "Head",
        "Neck",
        TORSO,
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
    private static final String DEFAULT_MODEL_RESOURCE = "/models/"+DEFAULT_MODEL_FILENAME;
    private static final Path DEFAULT_MODEL_DEV_PATH = Paths.get("src", "main", "resources", "models", DEFAULT_MODEL_FILENAME);

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

    private enum LightingPreset { TOP_DOWN, UNIFORM }

    private final Group lightsLayer = new Group();
    private LightingPreset lightingPreset = LightingPreset.UNIFORM;

    private final Group root3D = new Group(modelRoot);

    @FXML private BorderPane rootPane;
    @FXML private HBox toolbarBox;
    @FXML private Button loadModelButton;
    @FXML private Button resetViewButton;
    @FXML private Button exportModelButton;
    @FXML private ComboBox<LightingPreset> lightingCombo;
    @FXML private ScrollPane controlScroll;
    @FXML private VBox controlsContainer;
    @FXML private StackPane viewerPane;

    private boolean bootstrapped;

    private final javafx.scene.PerspectiveCamera camera = new javafx.scene.PerspectiveCamera(true);
    private final WorkspaceCamera cameraRig = new WorkspaceCamera(
        camera, MIN_DISTANCE, MAX_DISTANCE, ORBIT_SENSITIVITY, PAN_SENSITIVITY, PITCH_MIN, PITCH_MAX
    );
    private final TattooWorkspace tattooWorkspace = new TattooWorkspace();
    private final DoubleProperty overallScale = new SimpleDoubleProperty(1.0);

    private ModelManipulator modelManipulator;
    private Slider overallScaleSlider;
    private Stage primaryStage;

    private final ObservableList<TattooPreset> tattooHistory = FXCollections.observableArrayList();

    private Button loadTattooButton;
    private FlowPane tattooHistoryGallery;
    private ToggleGroup tattooHistoryToggleGroup;
    private ScrollPane tattooHistoryScroll;
    private Button applyHistoryButton;
    private Button deleteTattooButton;
    private Slider tattooSizeSlider;
    private Slider tattooOpacitySlider;
    private Slider tattooRotationSlider;
    private boolean tattooDragActive;
    private boolean modelHasUVs;

    private Bounds currentBounds;
    private SubScene subScene;
    private boolean adjustingSliders;

    public WorkspaceController() {
        initializePartGroups();
    }

    @FXML
    private void initialize() {
        setupToolbar();
        setupControlPanel();
        setupViewer();
        Platform.runLater(() -> {
            if (subScene != null) {
                subScene.requestFocus();
            }
        });
    }

    public void attachStage(Stage stage) {
        if (stage == null) {
            return;
        }
        this.primaryStage = stage;
        if (!bootstrapped) {
            bootstrapped = true;
            loadInitialModel();
        }
        Platform.runLater(() -> {
            if (subScene != null) {
                subScene.requestFocus();
            }
        });
    }

    private Stage resolveStage() {
        if (primaryStage != null) {
            return primaryStage;
        }
        if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
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

    private void setupToolbar() {
        if (loadModelButton != null) {
            loadModelButton.setOnAction(evt -> showLoadDialog(resolveStage()));
        }
        if (resetViewButton != null) {
            resetViewButton.setOnAction(evt -> cameraRig.reset());
        }
        if (exportModelButton != null) {
            exportModelButton.setOnAction(evt -> handleExportModel());
        }
        if (lightingCombo != null) {
            lightingCombo.setItems(FXCollections.observableArrayList(LightingPreset.values()));
            lightingCombo.setValue(lightingPreset);
            lightingCombo.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV != null && newV != lightingPreset) {
                    lightingPreset = newV;
                    applyLighting(lightingPreset);
                }
            });
        }
    }

    private void setupControlPanel() {
        if (controlsContainer == null) {
            return;
        }

        controlsContainer.getChildren().clear();
        controlsContainer.setSpacing(12);
        controlsContainer.setPadding(new Insets(16));
        controlsContainer.setFillWidth(true);

        loadTattooButton = new Button("Load Tattoo");
        loadTattooButton.setOnAction(e -> handleLoadTattoo());

        tattooHistoryToggleGroup = new ToggleGroup();
        tattooHistoryGallery = new FlowPane();
        tattooHistoryGallery.setHgap(6);
        tattooHistoryGallery.setVgap(6);
        tattooHistoryGallery.setPrefWrapLength(180);
        tattooHistoryGallery.getStyleClass().add("tattoo-history-gallery");

        tattooHistoryScroll = new ScrollPane(tattooHistoryGallery);
        tattooHistoryScroll.setFitToWidth(true);
        tattooHistoryScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tattooHistoryScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tattooHistoryScroll.setPrefViewportHeight(120);
        tattooHistoryScroll.setMaxHeight(150);
        tattooHistoryScroll.setFocusTraversable(false);
        tattooHistoryScroll.getStyleClass().add("tattoo-scroll");


        applyHistoryButton = new Button("Use Selected");
        applyHistoryButton.setOnAction(e -> applySelectedHistoryTattoo());
        applyHistoryButton.setMaxWidth(Double.MAX_VALUE);

        tattooSizeSlider = new Slider(0.05, 1.0, 0.20);
        tattooSizeSlider.getStyleClass().add(WP_SLIDER);
        tattooSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> onTattooAdjustment(t -> t.withScale(newVal.doubleValue())));

        tattooOpacitySlider = new Slider(0.3, 1.0, 1.0);
        tattooOpacitySlider.getStyleClass().add(WP_SLIDER);
        tattooOpacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> onTattooAdjustment(t -> t.withAlpha(newVal.doubleValue())));

        tattooRotationSlider = new Slider(-180.0, 180.0, 0.0);
        tattooRotationSlider.getStyleClass().add(WP_SLIDER);
        tattooRotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> onTattooAdjustment(t -> t.withRotation(newVal.doubleValue())));

        deleteTattooButton = new Button("Delete Tattoo");
        deleteTattooButton.setMaxWidth(Double.MAX_VALUE);
        deleteTattooButton.setOnAction(e -> handleDeleteTattoo());

        Label historyLabel = new Label("Recent Tattoos");
        VBox historyBox = new VBox(4, historyLabel, tattooHistoryScroll, applyHistoryButton);
        historyBox.setFillWidth(true);
        historyBox.setFocusTraversable(false);
        historyBox.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        
        VBox tattooControls = new VBox(6,
            loadTattooButton,
            historyBox,
            createLabeledControl("Tattoo Size", tattooSizeSlider),
            createLabeledControl("Tattoo Opacity", tattooOpacitySlider),
            createLabeledControl("Tattoo Rotation", tattooRotationSlider),
            deleteTattooButton
        );
        tattooControls.setFillWidth(true);

        controlsContainer.getChildren().add(createDropdownPanel("Tattoo Tools", tattooControls));

        proportionControls.clear();
        VBox proportionPanel = new VBox(8);
        proportionPanel.setFillWidth(true);
        for (String label : PROPORTION_KEYS) {
            Slider slider = createSlider(SLIDER_MIN, SLIDER_MAX);
            slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!adjustingSliders) {
                    applyCurrentProportions();
                }
            });
            proportionControls.put(label, slider);
            proportionPanel.getChildren().add(createLabeledControl(label, slider));
        }
        controlsContainer.getChildren().add(createDropdownPanel("Body Proportions", proportionPanel));

        Slider overallSlider = createSlider(OVERALL_MIN, OVERALL_MAX);
        overallSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!adjustingSliders) {
                overallScale.set(newVal.doubleValue());
            }
        });
        overallScaleSlider = overallSlider;
        controlsContainer.getChildren().add(createLabeledControl("Overall Scale", overallSlider));

        if (controlScroll != null) {
            controlScroll.setFitToWidth(true);
            controlScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            controlScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            controlScroll.setPadding(Insets.EMPTY);
            if (rootPane != null) {
                controlScroll.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.35));
                controlScroll.maxWidthProperty().bind(rootPane.widthProperty().multiply(0.35));
            }
        }

        refreshTattooHistoryGallery();
        updateTattooControlsState();
    }

    private void onTattooAdjustment(UnaryOperator<Tattoo> updater) {
        if (adjustingSliders) {
            return;
        }
        tattooWorkspace.updateSelection(updater);
    }

    private Slider createSlider(double min, double max) {
        Slider slider = new Slider(min, max, 1.0);
        slider.getStyleClass().add(WP_SLIDER);
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

    private TitledPane createDropdownPanel(String title, Node content) {
        TitledPane pane = new TitledPane(title, content);
        pane.setCollapsible(true);
        pane.setExpanded(false);
        pane.setFocusTraversable(false);
        pane.getStyleClass().add("sidebar-titled-pane");
        pane.setMaxWidth(Double.MAX_VALUE);
        return pane;
    }

    private void handleLoadTattoo() {
        if (!modelHasUVs) {
            showNoUVMessage();
            return;
        }
        Stage targetStage = resolveStage();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Tattoo Image");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", PNG_PATTERN, "*.jpg", "*.jpeg", "*.svg"),
            new FileChooser.ExtensionFilter("PNG", PNG_PATTERN),
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
        rememberTattoo(file, loaded);
        tattooWorkspace.preparePendingTattoo(loaded);
        updateTattooControlsState();
    }

    public boolean openTattooFromGallery(Image image, String label) {
        if (image == null) {
            return false;
        }
        if (!modelHasUVs) {
            showNoUVMessage();
            return false;
        }
        if (!tattooWorkspace.isPlacementAvailable()) {
            return false;
        }
        rememberTattoo(label, image);
        tattooWorkspace.preparePendingTattoo(image);
        updateTattooControlsState();
        return true;
    }

    private void handleExportModel() {
        if (viewerPane == null) {
            return;
        }
        Stage stage = resolveStage();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Workspace Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", PNG_PATTERN));
        chooser.setInitialFileName("workspace.png");
        File target = chooser.showSaveDialog(stage);
        if (target == null) {
            return;
        }
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = viewerPane.snapshot(params, null);
        if (snapshot == null) {
            showError("Unable to capture workspace snapshot", new IOException("Snapshot failed"));
            return;
        }
        try {
            ImageIO.write(convertToBufferedImage(snapshot), "png", target);
        } catch (IOException ex) {
            showError("Failed to export workspace image: " + target.getName(), ex);
        }
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
            showError("Failed to load tattoo image: " + file.getName(), ex instanceof IOException ioexception ? ioexception : new IOException(ex));
            return null;
        }
    }

    private void rememberTattoo(File source, Image image) {
        String label = source != null ? source.getName() : null;
        rememberTattoo(label, image);
    }

    private void rememberTattoo(String label, Image image) {
        if (image == null) {
            return;
        }
        String presetLabel = (label != null && !label.isBlank()) ? label : "Tattoo " + (tattooHistory.size() + 1);
        TattooPreset preset = new TattooPreset(presetLabel, image);
        tattooHistory.removeIf(existing -> existing.image() == image);
        tattooHistory.add(0, preset);
        while (tattooHistory.size() > MAX_TATTOO_HISTORY) {
            tattooHistory.remove(tattooHistory.size() - 1);
        }
        refreshTattooHistoryGallery();
    }

    private void refreshTattooHistoryGallery() {
        if (tattooHistoryGallery == null || tattooHistoryToggleGroup == null) {
            return;
        }
        TattooPreset previouslySelected = selectedHistoryPreset();
        tattooHistoryGallery.getChildren().clear();
        tattooHistoryToggleGroup.getToggles().clear();
        Toggle toggleToSelect = null;
        for (TattooPreset preset : tattooHistory) {
            ToggleButton button = createHistoryToggle(preset);
            tattooHistoryGallery.getChildren().add(button);
            tattooHistoryToggleGroup.getToggles().add(button);
            if (previouslySelected != null && previouslySelected.equals(preset)) {
                toggleToSelect = button;
            }
        }
        if (toggleToSelect != null) {
            toggleToSelect.setSelected(true);
        } else if (!tattooHistoryToggleGroup.getToggles().isEmpty()) {
            tattooHistoryToggleGroup.getToggles().get(0).setSelected(true);
        }
        updateTattooControlsState();
    }

    private ToggleButton createHistoryToggle(TattooPreset preset) {
        ImageView view = new ImageView(preset.image());
        view.setFitWidth(64);
        view.setFitHeight(64);
        view.setPreserveRatio(true);
        StackPane graphic = new StackPane(view);
        graphic.getStyleClass().add("tattoo-history-thumb");

        ToggleButton button = new ToggleButton();
        button.setGraphic(graphic);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        button.setPrefSize(74, 74);
        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        button.getStyleClass().add("tattoo-history-toggle");
        button.setUserData(preset);
        button.setOnAction(evt -> updateTattooControlsState());
        return button;
    }

    private TattooPreset selectedHistoryPreset() {
        if (tattooHistoryToggleGroup == null) {
            return null;
        }
        Toggle selected = tattooHistoryToggleGroup.getSelectedToggle();
        if (selected == null) {
            return null;
        }
        Object data = selected.getUserData();
        return data instanceof TattooPreset preset ? preset : null;
    }

    private void applySelectedHistoryTattoo() {
        if (!tattooWorkspace.isPlacementAvailable()) {
            return;
        }
        TattooPreset preset = selectedHistoryPreset();
        if (preset == null || preset.image() == null) {
            return;
        }
        tattooWorkspace.preparePendingTattoo(preset.image());
        updateTattooControlsState();
    }

    private void handleDeleteTattoo() {
        if (!tattooWorkspace.deleteSelectedTattoo()) {
            return;
        }
        var current = tattooWorkspace.selected();
        if (current.isPresent()) {
            syncTattooControls(current.get());
        } else {
            syncTattooControls(null);
        }
        updateTattooControlsState();
    }

    private void setupViewer() {
        if (viewerPane == null) {
            return;
        }

        subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#b4b4b4ff"));
        subScene.setCamera(camera);
        subScene.setFocusTraversable(true);

        installInteractionHandlers();
        installTattooPlacementHandlers();
        initLightingLayer(root3D);
        applyLighting(lightingPreset);
        softenSpecular(modelRoot);

        viewerPane.getChildren().setAll(subScene);
        StackPane.setMargin(subScene, Insets.EMPTY);

        subScene.widthProperty().bind(viewerPane.widthProperty());
        subScene.heightProperty().bind(viewerPane.heightProperty());

        if (rootPane != null) {
            viewerPane.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.65));
        }
    }

    private void installInteractionHandlers() {
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleCameraMousePressed);
        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleCameraMouseDragged);
        subScene.addEventHandler(ScrollEvent.SCROLL, cameraRig::handleScroll);
        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.MIDDLE || (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)) {
                cameraRig.reset();
            }
        });
        subScene.setOnKeyPressed(event -> {
            cameraRig.handleKey(event);
            if (event.getCode() == KeyCode.DIGIT1) {
                lightingPreset = LightingPreset.TOP_DOWN;
                applyLighting(lightingPreset);
            } else if (event.getCode() == KeyCode.DIGIT2) {
                lightingPreset = LightingPreset.UNIFORM;
                applyLighting(lightingPreset);
            }
        });
    }

    private void handleCameraMousePressed(MouseEvent event) {
        if (tattooDragActive && event.getButton() == MouseButton.PRIMARY) {
            event.consume();
            return;
        }
        cameraRig.handleMousePressed(event);
    }

    private void handleCameraMouseDragged(MouseEvent event) {
        if (tattooDragActive && event.getButton() == MouseButton.PRIMARY) {
            event.consume();
            return;
        }
        cameraRig.handleMouseDragged(event);
    }

    private void installTattooPlacementHandlers() {
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleTattooPress);
        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleTattooDrag);
        subScene.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleTattooRelease);
    }

    private void showLoadDialog(Stage stage) {
        Stage targetStage = stage != null ? stage : resolveStage();
        if (targetStage == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select OBJ Model");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wavefront OBJ", "*.obj"));
        Path initial = Paths.get(System.getProperty("user.dir"), "HumanModel.obj");
        if (Files.exists(initial)) {
            chooser.setInitialDirectory(initial.getParent().toFile());
        }

        File file = chooser.showOpenDialog(targetStage);
        if (file != null) {
            loadFromPath(file.toPath());
        }
    }

    private void initLightingLayer(Group sceneRoot) {
        if (!sceneRoot.getChildren().contains(lightsLayer)) {
            sceneRoot.getChildren().add(lightsLayer);
        }
    }

    private AmbientLight amb(double g) {
        return new AmbientLight(Color.gray(g));
    }

    private DirectionalLight dir(double g, double x, double y, double z) {
        DirectionalLight light = new DirectionalLight(Color.gray(g));
        light.setDirection(new javafx.geometry.Point3D(x, y, z).normalize());
        return light;
    }

    private void applyLighting(LightingPreset preset) {

    initLightingLayer(root3D);

    lightsLayer.getChildren().clear();

        if (preset == LightingPreset.TOP_DOWN) {
            lightsLayer.getChildren().addAll(
                amb(0.25),
                dir(1.0, 0, -1, 0)
            );
        } else if (preset == LightingPreset.UNIFORM) {
            lightsLayer.getChildren().addAll(
                amb(0.70),
                dir(0.35, -0.35, -1, -0.35),
                dir(0.25, 0.35, -1, 0.35)
            );
        }
    }

    private void softenSpecular(Parent root) {
        traverse(root, node -> {
            if (node instanceof Shape3D shape) {
                var material = shape.getMaterial();
                if (material instanceof PhongMaterial phong) {
                    phong.setSpecularColor(Color.gray(0.15));
                    phong.setSpecularPower(16);
                }
                shape.setCullFace(CullFace.BACK);
                shape.setDrawMode(DrawMode.FILL);
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

    private void handleTattooPress(MouseEvent event) {
        tattooDragActive = false;
        if (!tattooWorkspace.isPlacementAvailable()) {
            return;
        }
        if (event.getButton() == MouseButton.SECONDARY) {
            tattooWorkspace.clearSelection();
            syncTattooControls(null);
            updateTattooControlsState();
            return;
        }
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        Point2D uv = extractTextureCoord(event.getPickResult());
        if (uv == null) {
            return;
        }

        double u = clamp(uv.getX(), 0.0, 1.0);
        double v = clamp(uv.getY(), 0.0, 1.0);

        if (tattooWorkspace.selectTattooAt(u, v)) {
            tattooWorkspace.selected().ifPresent(this::syncTattooControls);
            updateTattooControlsState();
            tattooDragActive = true;
            event.consume();
            return;
        }

        if (!tattooWorkspace.hasPendingImage()) {
            tattooWorkspace.clearSelection();
            syncTattooControls(null);
            updateTattooControlsState();
            return;
        }

        Tattoo newTattoo = createTattooFromPending(u, v);
        if (newTattoo == null) {
            return;
        }
        tattooWorkspace.addTattoo(newTattoo);
        syncTattooControls(newTattoo);
        updateTattooControlsState();
        tattooDragActive = true;
        event.consume();
    }

    private void handleTattooDrag(MouseEvent event) {
        if (!tattooDragActive || !event.isPrimaryButtonDown()) {
            return;
        }
        if (!tattooWorkspace.isPlacementAvailable()) {
            return;
        }
        Tattoo current = tattooWorkspace.selected().orElse(null);
        if (current == null) {
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
        tattooWorkspace.updateSelectedTattoo(current.withUV(u, v));
        event.consume();
    }

    private void handleTattooRelease(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY || !event.isPrimaryButtonDown()) {
            tattooDragActive = false;
        }
    }

    private void syncTattooControls(Tattoo tattoo) {
        if (tattoo == null || tattooSizeSlider == null || tattooOpacitySlider == null || tattooRotationSlider == null) {
            return;
        }
        adjustingSliders = true;
        try {
            tattooSizeSlider.setValue(tattoo.scale());
            tattooOpacitySlider.setValue(tattoo.alpha());
            tattooRotationSlider.setValue(tattoo.rotation());
        } finally {
            adjustingSliders = false;
        }
    }

    private Point2D extractTextureCoord(PickResult result) {
        if (result == null || !(result.getIntersectedNode() instanceof MeshView)) {
            return null;
        }
        return result.getIntersectedTexCoord();
    }

    private Tattoo createTattooFromPending(double u, double v) {
        double size = sliderValueOrFallback(tattooSizeSlider, 0.2);
        double opacity = sliderValueOrFallback(tattooOpacitySlider, 1.0);
        double rotation = sliderValueOrFallback(tattooRotationSlider, 0.0);
        Image pending = tattooWorkspace.consumePendingImage().orElse(null);
        if (pending == null) {
            return null;
        }
        return new Tattoo(u, v, pending, size, rotation, opacity);
    }

    private double sliderValueOrFallback(Slider slider, double fallback) {
        return slider != null ? slider.getValue() : fallback;
    }

    private void loadInitialModel() {
        Path workingDirModel = Paths.get(System.getProperty("user.dir")).resolve(DEFAULT_MODEL_FILENAME);
        if (Files.exists(workingDirModel)) {
            loadFromPath(workingDirModel);
            return;
        }

        if (Files.exists(DEFAULT_MODEL_DEV_PATH)) {
            loadFromPath(DEFAULT_MODEL_DEV_PATH);
            return;
        }

        URL resource = getClass().getResource(DEFAULT_MODEL_RESOURCE);
        if (resource != null) {
            try (InputStream stream = resource.openStream()) {
                Path tempDir = Files.createTempDirectory(Path.of(System.getProperty("java.io.tmpdir")), "tattui-model-");
                tempDir.toFile().deleteOnExit();
                Path temp = Files.createTempFile(tempDir, "human-model", ".obj");
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
            cameraRig.reset();
            applyLighting(lightingPreset);
            softenSpecular(modelRoot);
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
        List<PhongMaterial> materials = new ArrayList<>();
        collectMaterials(modelRoot, materials);
        tattooWorkspace.configureMaterials(materials);
        tattooWorkspace.clearPendingTattoo();
        tattooWorkspace.clearAllTattoos();

        if (!modelHasUVs || materials.isEmpty()) {
            tattooWorkspace.clearSurface();
            updateTattooControlsState();
            return;
        }

        Image diffuse = null;
        for (PhongMaterial pm : materials) {
            if (pm.getDiffuseMap() != null) {
                diffuse = pm.getDiffuseMap();
                break;
            }
        }

        Image baseTexture;
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
        tattooWorkspace.configureSurface(baseTexture, texWidth, texHeight);
        updateTattooControlsState();
    }

    private void collectMaterials(Node node, List<PhongMaterial> out) {
        if ((node instanceof Shape3D shape && shape.getMaterial() instanceof PhongMaterial pm) && !out.contains(pm)) {
            out.add(pm);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectMaterials(child, out);
            }
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
        boolean placementEnabled = tattooWorkspace.isPlacementAvailable();
        boolean hasSelection = tattooWorkspace.selected().isPresent();
        boolean hasHistory = !tattooHistory.isEmpty();
        boolean slidersEnabled = placementEnabled && hasSelection;
        if (loadTattooButton != null) {
            loadTattooButton.setDisable(!modelHasUVs);
        }
        if (tattooSizeSlider != null) {
            tattooSizeSlider.setDisable(!slidersEnabled);
        }
        if (tattooOpacitySlider != null) {
            tattooOpacitySlider.setDisable(!slidersEnabled);
        }
        if (tattooRotationSlider != null) {
            tattooRotationSlider.setDisable(!slidersEnabled);
        }
        if (tattooHistoryScroll != null) {
            tattooHistoryScroll.setDisable(!hasHistory);
        }
        if (applyHistoryButton != null) {
            boolean canApply = placementEnabled && selectedHistoryPreset() != null;
            applyHistoryButton.setDisable(!canApply);
        }
        if (deleteTattooButton != null) {
            deleteTattooButton.setDisable(!hasSelection);
        }
    }

    private void updateCurrentBounds() {
        currentBounds = overallScaleGroup.getBoundsInParent();
        cameraRig.setBounds(currentBounds);
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
        parts.add(new ObjLoader.ModelPart(TORSO, torso));

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
        return Objects.requireNonNull(partGroups.get(TORSO));
    }

    private BufferedImage convertToBufferedImage(WritableImage image) {
        int width = (int) Math.max(1, Math.round(image.getWidth()));
        int height = (int) Math.max(1, Math.round(image.getHeight()));
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return buffered;
    }

    private double clamp(double value, double min, double max) {
        return Math.clamp(value, min, max);
    }

    private record TattooPreset(String label, Image image) {
        @Override
        public String toString() {
            return label;
        }
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
}
