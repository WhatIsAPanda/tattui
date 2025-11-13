package app.boundary;

import app.controller.WorkspaceController;
import app.controller.workspace.TattooWorkspace;
import app.controller.workspace.WorkspaceCamera;
import app.entity.CameraState;
import app.entity.Tattoo;
import app.loader.ObjLoader;
import app.view3d.LightingSystem;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.fxml.FXML;
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
import javafx.scene.control.SplitPane;
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
import javafx.scene.input.KeyEvent;
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
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Application controller that encapsulates the workspace behaviour and state.
 */
public final class WorkspaceBoundary implements WorkspaceController {
    private static final String PNG_PATTERN = "*.png";
    private static final double TARGET_HEIGHT = 1700.0;
    private static final double MIN_DISTANCE = 300.0;
    private static final double MAX_DISTANCE = 6000.0;
    private static final double TATTOO_MIN_SCALE = 0.05;
    private static final double TATTOO_MAX_SCALE = 1.0;
    private static final double DEFAULT_TATTOO_SCALE = 0.20;
    private static final double ORBIT_SENSITIVITY = 0.3;
    private static final double PAN_SENSITIVITY = 0.5;
    private static final double PITCH_MIN = -80.0;
    private static final double PITCH_MAX = 80.0;
    private static final int MAX_TATTOO_HISTORY = 12;
    private static final Color DEFAULT_SKIN_TONE = Color.rgb(224, 172, 105);
    private static final List<Color> SKIN_TONE_PALETTE = List.of(
        Color.rgb(110, 66, 24),   // Darkest
        Color.rgb(120, 72, 28),
        Color.rgb(133, 80, 32),
        Color.rgb(146, 90, 36),
        Color.rgb(156, 97, 40),
        Color.rgb(163, 103, 42),
        Color.rgb(170, 108, 45),
        Color.rgb(177, 114, 49),
        Color.rgb(185, 121, 55),
        Color.rgb(193, 128, 60),
        Color.rgb(198, 134, 66),
        Color.rgb(205, 143, 75),
        Color.rgb(213, 153, 85),
        Color.rgb(219, 163, 94),
        Color.rgb(224, 172, 105),
        Color.rgb(229, 178, 111),
        Color.rgb(233, 183, 115),
        Color.rgb(237, 189, 120),
        Color.rgb(241, 194, 125),
        Color.rgb(244, 201, 136),
        Color.rgb(250, 212, 153),
        Color.rgb(255, 219, 172),
        Color.rgb(255, 227, 190),
        Color.rgb(255, 234, 205),
        Color.rgb(255, 237, 211),
        Color.rgb(255, 242, 224),
        Color.rgb(255, 247, 236)
    );
    public static final String TORSO = "Torso";
    public static final String WP_SLIDER = "workspace-slider";
    public static final String VERSION = "version";
    public static final String DOT_IMAGE = ".image";
    public static final String DOT_SCALE = ".scale";

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

    private final Map<String, Group> partGroups = new LinkedHashMap<>();

    private final Group partRoot = new Group();
    private final Group overallScaleGroup = new Group(partRoot);
    private final Group modelRoot = new Group(overallScaleGroup);

    private enum TattooDimension { WIDTH, HEIGHT }

    private final Group root3D = new Group(modelRoot);
    private final LightingSystem lightingSystem = new LightingSystem();
    private LightingSystem.Mode lightingMode = LightingSystem.Mode.UNLIT;

    @FXML private BorderPane rootPane;
    @FXML private HBox toolbarBox;
    @FXML private Button loadModelButton;
    @FXML private Button loadProjectButton;
    @FXML private Button resetViewButton;
    @FXML private Button exportPreviewButton;
    @FXML private Button exportProjectButton;
    private ComboBox<LightingSystem.Mode> lightingModeCombo;
    @FXML private SplitPane contentSplit;
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
    private final ObjectProperty<Color> skinTone = new SimpleObjectProperty<>(DEFAULT_SKIN_TONE);

    private Stage primaryStage;
    private Path currentModelPath;
    private Path extractedProjectDir;

    private final ObservableList<TattooPreset> tattooHistory = FXCollections.observableArrayList();

    private Button loadTattooButton;
    private Button removeBackgroundButton;
    private Button invertTattooButton;
    private Button reflectTattooButton;
    private FlowPane tattooHistoryGallery;
    private ToggleGroup tattooHistoryToggleGroup;
    private ScrollPane tattooHistoryScroll;
    private Button deleteTattooButton;
    private Slider tattooWidthSlider;
    private Slider tattooHeightSlider;
    private Slider tattooOpacitySlider;
    private Slider tattooRotationSlider;
    private ToggleGroup skinToneToggleGroup;
    private ToggleButton lockAspectRatioToggle;
    private boolean tattooDragActive;
    private double tattooDragOffsetU;
    private double tattooDragOffsetV;
    private boolean historyPlacementArmed;
    private boolean modelHasUVs;
    private boolean modelProvidesBaseTexture;

    private final List<PhongMaterial> activeMaterials = new ArrayList<>();

    private Bounds currentBounds;
    private SubScene subScene;
    private boolean adjustingSliders;

    public WorkspaceBoundary() {
        initializePartGroups();
    }

    @FXML
    private void initialize() {
        setupToolbar();
        setupControlPanel();
        setupViewer();
        configureSplitPane();
        if (rootPane != null) {
            rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleWorkspaceKey);
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleWorkspaceKey);
                } else if (oldScene != null) {
                    oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleWorkspaceKey);
                }
            });
        }
        skinTone.addListener((obs, oldColor, newColor) -> {
            applySkinToneToMaterials();
            tattooWorkspace.updateSkinTone(newColor);
            syncSkinToneSelection();
        });
        syncSkinToneSelection();
        Platform.runLater(() -> {
            if (subScene != null) {
                subScene.requestFocus();
            }
        });
    }

    @Override
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
        if (loadProjectButton != null) {
            loadProjectButton.setOnAction(evt -> handleLoadProject());
        }
        if (resetViewButton != null) {
            resetViewButton.setOnAction(evt -> cameraRig.reset());
        }
        if (exportPreviewButton != null) {
            exportPreviewButton.setOnAction(evt -> handleExportPreview());
        }
        if (exportProjectButton != null) {
            exportProjectButton.setOnAction(evt -> handleExportProject());
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
        controlsContainer.setMinWidth(0);

        loadTattooButton = new Button("Load Tattoo");
        loadTattooButton.setOnAction(e -> handleLoadTattoo());
        removeBackgroundButton = new Button("Remove BG");
        removeBackgroundButton.setOnAction(e -> handleRemoveBackground());
        invertTattooButton = new Button("Invert Colors");
        invertTattooButton.setOnAction(e -> handleInvertTattoo());
        reflectTattooButton = new Button("Reflect Y-Axis");
        reflectTattooButton.setOnAction(e -> handleReflectTattoo());
        HBox tattooLoadRow = new HBox(10, loadTattooButton, createSpacer(), removeBackgroundButton, invertTattooButton, reflectTattooButton);
        tattooLoadRow.setFillHeight(true);

        lightingModeCombo = new ComboBox<>();
        lightingModeCombo.setItems(FXCollections.observableArrayList(LightingSystem.Mode.values()));
        lightingModeCombo.setValue(lightingMode);
        lightingModeCombo.valueProperty().addListener((obs, oldMode, newMode) -> {
            if (newMode != null && newMode != lightingMode) {
                lightingMode = newMode;
                lightingSystem.apply(newMode);
            }
        });

        skinToneToggleGroup = new ToggleGroup();

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

        tattooWidthSlider = new Slider(TATTOO_MIN_SCALE, TATTOO_MAX_SCALE, DEFAULT_TATTOO_SCALE);
        tattooWidthSlider.getStyleClass().add(WP_SLIDER);
        tattooWidthSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            onTattooDimensionChange(TattooDimension.WIDTH, tattooWidthSlider, newVal.doubleValue())
        );

        tattooHeightSlider = new Slider(TATTOO_MIN_SCALE, TATTOO_MAX_SCALE, DEFAULT_TATTOO_SCALE);
        tattooHeightSlider.getStyleClass().add(WP_SLIDER);
        tattooHeightSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            onTattooDimensionChange(TattooDimension.HEIGHT, tattooHeightSlider, newVal.doubleValue())
        );

        tattooOpacitySlider = new Slider(0.3, 1.0, 1.0);
        tattooOpacitySlider.getStyleClass().add(WP_SLIDER);
        tattooOpacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> onTattooAdjustment(t -> t.withAlpha(newVal.doubleValue())));

        tattooRotationSlider = new Slider(-180.0, 180.0, 0.0);
        tattooRotationSlider.getStyleClass().add(WP_SLIDER);
        tattooRotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> onTattooAdjustment(t -> t.withRotation(newVal.doubleValue())));

        deleteTattooButton = new Button("Delete Tattoo");
        deleteTattooButton.setMaxWidth(Double.MAX_VALUE);
        deleteTattooButton.setOnAction(e -> handleDeleteTattoo());

        lockAspectRatioToggle = new ToggleButton("Lock Aspect Ratio");
        lockAspectRatioToggle.setMaxWidth(Double.MAX_VALUE);
        lockAspectRatioToggle.getStyleClass().add("lock-aspect-toggle");
        lockAspectRatioToggle.selectedProperty().addListener((obs, oldVal, newVal) -> handleAspectLockChange(newVal));

        Label historyLabel = new Label("Loaded Tattoos");
        VBox historyBox = new VBox(4, historyLabel, tattooHistoryScroll);
        historyBox.setFillWidth(true);
        historyBox.setFocusTraversable(false);
        historyBox.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        historyBox.getStyleClass().add("sidebar-card");
        
        VBox tattooControls = new VBox(6,
            tattooLoadRow,
            historyBox,
            createLabeledControl("Tattoo Width", tattooWidthSlider),
            createLabeledControl("Tattoo Height", tattooHeightSlider),
            lockAspectRatioToggle,
            createLabeledControl("Tattoo Opacity", tattooOpacitySlider),
            createLabeledControl("Tattoo Rotation", tattooRotationSlider),
            deleteTattooButton
        );
        tattooControls.setFillWidth(true);

        controlsContainer.getChildren().add(createDropdownPanel("Tattoo Tools", tattooControls));

        VBox modelProperties = new VBox(6,
            buildSkinToneSelector(),
            buildLightingSelector()
        );
        modelProperties.setFillWidth(true);

        controlsContainer.getChildren().add(createDropdownPanel("Model Properties", modelProperties));
        syncSkinToneSelection();

        if (controlScroll != null) {
            controlScroll.setFitToWidth(true);
            controlScroll.setFitToHeight(true);
            controlScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            controlScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            controlScroll.setPadding(Insets.EMPTY);
            controlScroll.setMinWidth(0);
            controlScroll.setMaxWidth(Double.MAX_VALUE);
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

    private void onTattooDimensionChange(TattooDimension dimension, Slider source, double value) {
        if (adjustingSliders) {
            return;
        }
        if (isAspectRatioLocked()) {
            syncLockedTattooSliders(value, source);
            onTattooAdjustment(t -> t.withUniformScale(value));
            return;
        }
        if (dimension == TattooDimension.WIDTH) {
            onTattooAdjustment(t -> t.withWidthScale(value));
        } else {
            onTattooAdjustment(t -> t.withHeightScale(value));
        }
    }

    private void handleAspectLockChange(boolean locked) {
        if (!locked || adjustingSliders) {
            return;
        }
        double width = sliderValueOrFallback(tattooWidthSlider, DEFAULT_TATTOO_SCALE);
        double height = sliderValueOrFallback(tattooHeightSlider, DEFAULT_TATTOO_SCALE);
        double uniform = (width + height) * 0.5;
        syncLockedTattooSliders(uniform, null);
        onTattooAdjustment(t -> t.withUniformScale(uniform));
    }

    private void syncLockedTattooSliders(double value, Slider source) {
        adjustingSliders = true;
        try {
            if (tattooWidthSlider != null && tattooWidthSlider != source) {
                tattooWidthSlider.setValue(value);
            }
            if (tattooHeightSlider != null && tattooHeightSlider != source) {
                tattooHeightSlider.setValue(value);
            }
        } finally {
            adjustingSliders = false;
        }
    }

    private boolean isAspectRatioLocked() {
        return lockAspectRatioToggle != null && lockAspectRatioToggle.isSelected();
    }

    private void resetTattooAdjustmentControls() {
        adjustingSliders = true;
        try {
            if (tattooWidthSlider != null) {
                tattooWidthSlider.setValue(DEFAULT_TATTOO_SCALE);
            }
            if (tattooHeightSlider != null) {
                tattooHeightSlider.setValue(DEFAULT_TATTOO_SCALE);
            }
            if (tattooOpacitySlider != null) {
                tattooOpacitySlider.setValue(1.0);
            }
            if (tattooRotationSlider != null) {
                tattooRotationSlider.setValue(0.0);
            }
        } finally {
            adjustingSliders = false;
        }
    }

    private VBox createLabeledControl(String labelText, Slider slider) {
        Label label = new Label(labelText);
        VBox box = new VBox(4, label, slider);
        box.setFillWidth(true);
        return box;
    }

    private VBox buildSkinToneSelector() {
        Label label = new Label("Skin Tone");
        FlowPane palette = new FlowPane(8, 8);
        palette.setPrefWrapLength(200);
        palette.setRowValignment(VPos.CENTER);
        palette.setColumnHalignment(HPos.LEFT);
        palette.setAlignment(Pos.CENTER_LEFT);
        for (Color tone : SKIN_TONE_PALETTE) {
            palette.getChildren().add(createSkinToneButton(tone));
        }
        VBox container = new VBox(4, label, palette);
        container.setFillWidth(true);
        return container;
    }

    private ToggleButton createSkinToneButton(Color tone) {
        ToggleButton button = new ToggleButton();
        button.setMinSize(32, 22);
        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        button.getStyleClass().add("skin-tone-chip");
        button.setUserData(tone);
        button.setToggleGroup(skinToneToggleGroup);
        button.setStyle("-fx-background-color: " + toWebColor(tone) + ";");
        button.setOnAction(e -> {
            skinToneToggleGroup.selectToggle(button);
            if (!Objects.equals(skinTone.get(), tone)) {
                skinTone.set(tone);
            }
        });
        if (Objects.equals(tone, skinTone.get()) && skinToneToggleGroup.getSelectedToggle() == null) {
            skinToneToggleGroup.selectToggle(button);
        }
        return button;
    }

    private VBox buildLightingSelector() {
        Label label = new Label("Lighting Mode");
        HBox row = new HBox(8, lightingModeCombo);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setFillHeight(true);
        HBox.setHgrow(lightingModeCombo, Priority.ALWAYS);
        return new VBox(4, label, row);
    }

    private String toWebColor(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private TitledPane createDropdownPanel(String title, Node content) {
        TitledPane pane = new TitledPane(title, content);
        pane.setCollapsible(true);
        pane.setExpanded(true);
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
        resetTattooAdjustmentControls();
        rememberTattoo(file, loaded);
        historyPlacementArmed = false;
        tattooWorkspace.clearPendingTattoo();
        if (tattooHistoryToggleGroup != null) {
            tattooHistoryToggleGroup.selectToggle(null);
        }
        updateTattooControlsState();
    }

    private void handleRemoveBackground() {
        TattooPreset preset = selectedHistoryPreset();
        if (preset == null || preset.image() == null) {
            return;
        }
        Image processed = removeBackground(preset.image());
        if (processed == null) {
            return;
        }
        String label = preset.label() + " (No BG)";
        rememberTattoo(label, processed);
        if (tattooHistoryToggleGroup != null) {
            tattooHistoryToggleGroup.selectToggle(null);
        }
        historyPlacementArmed = false;
        tattooWorkspace.clearPendingTattoo();
        updateTattooControlsState();
    }

    private void handleInvertTattoo() {
        TattooPreset preset = selectedHistoryPreset();
        if (preset == null || preset.image() == null) {
            return;
        }
        Image inverted = invertImageColors(preset.image());
        if (inverted == null) {
            return;
        }
        String label = preset.label() + " (Inverted)";
        rememberTattoo(label, inverted);
        if (tattooHistoryToggleGroup != null) {
            tattooHistoryToggleGroup.selectToggle(null);
        }
        historyPlacementArmed = false;
        tattooWorkspace.clearPendingTattoo();
        updateTattooControlsState();
    }

    private void handleReflectTattoo() {
        TattooPreset preset = selectedHistoryPreset();
        if (preset == null || preset.image() == null) {
            return;
        }
        Image reflected = reflectImageY(preset.image());
        if (reflected == null) {
            return;
        }
        String label = preset.label() + " (Reflected)";
        rememberTattoo(label, reflected);
        if (tattooHistoryToggleGroup != null) {
            tattooHistoryToggleGroup.selectToggle(null);
        }
        historyPlacementArmed = false;
        tattooWorkspace.clearPendingTattoo();
        updateTattooControlsState();
    }

    @Override
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
        resetTattooAdjustmentControls();
        rememberTattoo(label, image);
        historyPlacementArmed = false;
        tattooWorkspace.clearPendingTattoo();
        if (tattooHistoryToggleGroup != null) {
            tattooHistoryToggleGroup.selectToggle(null);
        }
        updateTattooControlsState();
        return true;
    }

    private void handleLoadProject() {
        Stage stage = resolveStage();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Project");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tattui Project Archive", "*.zip"));
        File archive = chooser.showOpenDialog(stage);
        if (archive == null) {
            return;
        }
        Path extraction = null;
        try {
            extraction = extractProjectArchive(archive.toPath());
            Path objPath = findFirstObj(extraction);
            if (objPath == null) {
                throw new IOException("Project archive does not contain an OBJ file.");
            }
            loadFromPath(objPath);
            if (Objects.equals(currentModelPath, objPath)) {
                replaceExtractedProjectDir(extraction);
                extraction = null;
            }
        } catch (IOException ex) {
            showError("Failed to load project archive: " + archive.getName(), ex);
        } finally {
            if (extraction != null) {
                deleteRecursively(extraction);
            }
        }
    }

    private void handleExportPreview() {
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

    private void handleExportProject() {
        if (currentModelPath == null || !Files.exists(currentModelPath)) {
            showError("Export unavailable", new IOException("No model file is currently loaded."));
            return;
        }
        if (!tattooWorkspace.isPlacementAvailable()) {
            showError("Export unavailable", new IOException("Tattoo surface is not available for this model."));
            return;
        }
        Stage stage = resolveStage();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Project");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tattui Project Archive", "*.zip"));
        String suggested = stripExtension(currentModelPath.getFileName().toString()) + "_project.zip";
        chooser.setInitialFileName(suggested);
        File target = chooser.showSaveDialog(stage);
        if (target == null) {
            return;
        }
        try {
            exportProjectArchive(target.toPath());
        } catch (IOException ex) {
            showError("Failed to export project", ex);
        }
    }

    private void exportProjectArchive(Path requestedTarget) throws IOException {
        if (requestedTarget == null) {
            throw new IOException("No export target specified");
        }
        Path zipTarget = ensureZipExtension(requestedTarget);
        Path exportDir = Files.createTempDirectory("tattui-export-");
        try {
            String sourceFileName = currentModelPath != null ? currentModelPath.getFileName().toString() : "model.obj";
            Path objTarget = exportDir.resolve(sourceFileName);
            writeProjectAssets(currentModelPath, objTarget);
            zipDirectory(exportDir, zipTarget);
        } finally {
            deleteRecursively(exportDir);
        }
    }

    private void writeProjectAssets(Path sourceObj, Path targetObj) throws IOException {
        if (sourceObj == null || targetObj == null) {
            throw new IOException("Invalid export paths");
        }
        Path parent = targetObj.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(sourceObj, targetObj, StandardCopyOption.REPLACE_EXISTING);
        copyModelDependencies(sourceObj, parent);
        Path metadataPath = metadataPathFor(targetObj);
        Path preferencesPath = preferencesPathFor(targetObj);
        Path tattooDir = targetObj.resolveSibling(stripExtension(targetObj.getFileName().toString()) + "_tattoos");
        Path baseTexturePath = tattooDir.resolve("base.png");
        Path writtenBaseTexture = writeBaseTextureImage(baseTexturePath);
        List<Tattoo> exportedTattoos = tattooWorkspace.exportableTattoos();
        List<TattooPreset> historySnapshot = snapshotTattooHistory();
        writeTattooMetadata(
            metadataPath,
            tattooDir,
            exportedTattoos,
            historySnapshot,
            writtenBaseTexture
        );
        writeWorkspacePreferences(preferencesPath, captureWorkspacePreferences());
    }

    private void copyModelDependencies(Path sourceObj, Path targetDir) throws IOException {
        if (sourceObj == null || targetDir == null) {
            return;
        }
        Path sourceParent = sourceObj.getParent();
        if (sourceParent == null) {
            return;
        }
        List<String> lines = Files.readAllLines(sourceObj, StandardCharsets.UTF_8);
        Set<String> referenced = new LinkedHashSet<>();
        List<CopiedMaterial> copiedMaterials = new ArrayList<>();
        for (String line : lines) {
            processMaterialDirective(line, referenced, copiedMaterials, sourceParent, targetDir);
        }
        copyMaterialTextures(copiedMaterials, targetDir);
    }

    private void processMaterialDirective(
        String line,
        Set<String> referenced,
        List<CopiedMaterial> copiedMaterials,
        Path sourceParent,
        Path targetDir
    ) throws IOException {
        if (!isMtllibDirective(line)) {
            return;
        }
        String[] tokens = line.trim().split("\\s+");
        for (int i = 1; i < tokens.length; i++) {
            addMaterialReference(tokens[i], referenced, copiedMaterials, sourceParent, targetDir);
        }
    }

    private boolean isMtllibDirective(String line) {
        return line != null && line.trim().toLowerCase(Locale.ROOT).startsWith("mtllib");
    }

    private void addMaterialReference(
        String token,
        Set<String> referenced,
        List<CopiedMaterial> copiedMaterials,
        Path sourceParent,
        Path targetDir
    ) throws IOException {
        String reference = token.trim();
        if (reference.isEmpty() || !referenced.add(reference)) {
            return;
        }
        CopiedMaterial copied = copyMaterialFile(sourceParent, targetDir, reference);
        if (copied != null) {
            copiedMaterials.add(copied);
        }
    }

    private void copyMaterialTextures(List<CopiedMaterial> materials, Path exportRoot) throws IOException {
        for (CopiedMaterial material : materials) {
            copyMaterialTextures(material, exportRoot);
        }
    }

    private CopiedMaterial copyMaterialFile(Path sourceParent, Path targetParent, String reference) throws IOException {
        Path relative = parseReference(reference);
        if (relative == null || sourceParent == null) {
            return null;
        }
        Path sourcePath = sourceParent.resolve(relative).normalize();
        if (!Files.exists(sourcePath)) {
            return null;
        }
        Path targetPath = targetParent.resolve(relative).normalize();
        ensureWithinDirectory(targetParent, targetPath);
        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return new CopiedMaterial(sourcePath, targetPath);
    }

    private void copyMaterialTextures(CopiedMaterial material, Path exportRoot) throws IOException {
        Path source = material.source();
        Path target = material.target();
        if (source == null || target == null || !Files.exists(source)) {
            return;
        }
        Path sourceDir = source.getParent();
        Path targetDir = target.getParent();
        if (sourceDir == null || targetDir == null) {
            return;
        }
        List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        Set<String> copied = new LinkedHashSet<>();
        for (String line : lines) {
            String reference = resolveTextureReference(line, copied);
            if (reference != null) {
                copyRelatedResource(sourceDir, targetDir, exportRoot, reference);
            }
        }
    }

    private String resolveTextureReference(String line, Set<String> copied) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        boolean textureDirective = lower.startsWith("map_")
            || lower.startsWith("bump")
            || lower.startsWith("disp")
            || lower.startsWith("refl");
        if (!textureDirective) {
            return null;
        }
        String reference = extractTextureReference(trimmed);
        if (reference == null || !copied.add(reference)) {
            return null;
        }
        return reference;
    }

    private void copyRelatedResource(Path sourceBase, Path targetBase, Path exportRoot, String reference) throws IOException {
        Path relative = parseReference(reference);
        if (relative == null || sourceBase == null || targetBase == null) {
            return;
        }
        Path sourcePath = sourceBase.resolve(relative).normalize();
        if (!Files.exists(sourcePath)) {
            return;
        }
        Path targetPath = targetBase.resolve(relative).normalize();
        ensureWithinDirectory(exportRoot, targetPath);
        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private String extractTextureReference(String line) {
        String[] tokens = line.split("\\s+");
        for (int i = tokens.length - 1; i >= 1; i--) {
            String token = tokens[i].trim();
            if (!token.isEmpty() && !token.startsWith("-")) {
                return token;
            }
        }
        return null;
    }

    private Path parseReference(String reference) {
        if (reference == null) {
            return null;
        }
        String sanitized = reference.replace('\\', File.separatorChar).trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        try {
            Path path = Paths.get(sanitized);
            return path.isAbsolute() ? null : path;
        } catch (InvalidPathException ex) {
            return null;
        }
    }

    private void ensureWithinDirectory(Path root, Path candidate) throws IOException {
        if (root == null || candidate == null) {
            return;
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedRoot)) {
            throw new IOException("Resource resolves outside export directory: " + candidate);
        }
    }

    private void zipDirectory(Path sourceDir, Path zipTarget) throws IOException {
        if (sourceDir == null || zipTarget == null) {
            throw new IOException("Invalid archive paths");
        }
        List<Path> files;
        try (Stream<Path> walk = Files.walk(sourceDir)) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        if (files.isEmpty()) {
            throw new IOException("No files available for export");
        }
        if (zipTarget.getParent() != null) {
            Files.createDirectories(zipTarget.getParent());
        }
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zipTarget))) {
            for (Path file : files) {
                Path relative = sourceDir.relativize(file);
                ZipEntry entry = new ZipEntry(relative.toString().replace('\\', '/'));
                out.putNextEntry(entry);
                Files.copy(file, out);
                out.closeEntry();
            }
        }
    }

    private Path extractProjectArchive(Path archive) throws IOException {
        if (archive == null || !Files.exists(archive)) {
            throw new IOException("Archive not found");
        }
        Path tempDir = Files.createTempDirectory("tattui-project-");
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                Path resolved = tempDir.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(tempDir)) {
                    throw new IOException("Archive entry resolves outside target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    if (resolved.getParent() != null) {
                        Files.createDirectories(resolved.getParent());
                    }
                    Files.copy(in, resolved, StandardCopyOption.REPLACE_EXISTING);
                }
                in.closeEntry();
            }
            return tempDir;
        } catch (IOException ex) {
            deleteRecursively(tempDir);
            throw ex;
        }
    }

    private Path findFirstObj(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) {
            return null;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj"))
                .findFirst()
                .orElse(null);
        }
    }

    private Path ensureZipExtension(Path target) {
        if (target == null) {
            return null;
        }
        String name = target.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return target;
        }
        return target.resolveSibling(name + ".zip");
    }

    private void replaceExtractedProjectDir(Path newDir) {
        if (Objects.equals(extractedProjectDir, newDir)) {
            return;
        }
        deleteRecursively(extractedProjectDir);
        extractedProjectDir = newDir;
    }

    private void cleanupExtractionIfNotNeeded(Path modelPath) {
        if (extractedProjectDir == null || modelPath == null) {
            return;
        }
        Path normalizedModel = modelPath.toAbsolutePath().normalize();
        Path extractionRoot = extractedProjectDir.toAbsolutePath().normalize();
        if (!normalizedModel.startsWith(extractionRoot)) {
            deleteRecursively(extractedProjectDir);
            extractedProjectDir = null;
        }
    }

    private void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(candidate -> {
                try {
                    Files.deleteIfExists(candidate);
                } catch (IOException ignored) {
                    // Swallow cleanup failures silently.
                }
            });
        } catch (IOException ignored) {
            // Ignore cleanup failures.
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

    private List<TattooPreset> snapshotTattooHistory() {
        List<TattooPreset> snapshot = new ArrayList<>();
        int limit = Math.min(MAX_TATTOO_HISTORY, tattooHistory.size());
        for (int i = 0; i < limit; i++) {
            snapshot.add(tattooHistory.get(i));
        }
        return snapshot;
    }

    private void refreshTattooHistoryGallery() {
        if (tattooHistoryGallery == null || tattooHistoryToggleGroup == null) {
            return;
        }
        tattooHistoryGallery.getChildren().clear();
        tattooHistoryToggleGroup.getToggles().clear();
        for (TattooPreset preset : tattooHistory) {
            ToggleButton button = createHistoryToggle(preset);
            tattooHistoryGallery.getChildren().add(button);
            tattooHistoryToggleGroup.getToggles().add(button);
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
        button.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                applySelectedHistoryTattoo();
            } else if (tattooHistoryToggleGroup != null && tattooHistoryToggleGroup.getSelectedToggle() == null) {
                disarmHistorySelection();
            }
            updateTattooControlsState();
        });
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
        resetTattooAdjustmentControls();
        tattooWorkspace.preparePendingTattoo(preset.image());
        historyPlacementArmed = true;
        updateTattooControlsState();
    }

    private void disarmHistorySelection() {
        historyPlacementArmed = false;
        tattooWorkspace.clearPendingTattoo();
    }

    private void captureDragOffsets(Tattoo tattoo, double hitU, double hitV) {
        tattooDragOffsetU = hitU - tattoo.u();
        tattooDragOffsetV = hitV - tattoo.v();
    }

    private void resetDragOffsets() {
        tattooDragOffsetU = 0.0;
        tattooDragOffsetV = 0.0;
    }

    private boolean handleDeleteTattoo() {
        if (!tattooWorkspace.deleteSelectedTattoo()) {
            return false;
        }
        tattooWorkspace.selected().ifPresentOrElse(this::syncTattooControls, () -> syncTattooControls(null));
        updateTattooControlsState();
        return true;
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
        if (!root3D.getChildren().contains(lightingSystem.node())) {
            root3D.getChildren().add(lightingSystem.node());
        }
        refreshLightingRig();
        softenSpecular(modelRoot);

        viewerPane.getChildren().setAll(subScene);
        StackPane.setMargin(subScene, Insets.EMPTY);

        viewerPane.setMinSize(0, 0);
        viewerPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        subScene.widthProperty().bind(viewerPane.widthProperty());
        subScene.heightProperty().bind(viewerPane.heightProperty());
    }

    private void configureSplitPane() {
        if (contentSplit == null) {
            return;
        }
        contentSplit.setMinSize(0, 0);
        contentSplit.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contentSplit.setPickOnBounds(false);
        if (viewerPane != null) {
            SplitPane.setResizableWithParent(viewerPane, true);
        }
        if (controlScroll != null) {
            SplitPane.setResizableWithParent(controlScroll, true);
        }
        Platform.runLater(() -> contentSplit.setDividerPositions(0.68));
    }

    private void refreshLightingRig() {
        lightingSystem.reset();
        lightingSystem.apply(lightingMode);
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
        subScene.setOnKeyPressed(this::handleWorkspaceKey);
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

    private void handleWorkspaceKey(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            if (tattooWorkspace.selected().isPresent()) {
                handleDeleteTattoo();
            }
            return;
        }
        cameraRig.handleKey(event);
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


    private void softenSpecular(Parent root) {
        traverse(root, node -> {
            if (node instanceof Shape3D shape) {
                shape.setCullFace(CullFace.BACK);
                shape.setDrawMode(DrawMode.FILL);
            }
        });
    }

    private void initializeSkinToneFromMaterials() {
        Color selectedTone = SKIN_TONE_PALETTE.isEmpty() ? DEFAULT_SKIN_TONE : SKIN_TONE_PALETTE.get(SKIN_TONE_PALETTE.size() - 1);
        skinTone.set(selectedTone);
        applySkinToneToMaterials();
        tattooWorkspace.updateSkinTone(selectedTone);
        if (skinToneToggleGroup != null && !skinToneToggleGroup.getToggles().isEmpty()) {
            skinToneToggleGroup.selectToggle(skinToneToggleGroup.getToggles().get(skinToneToggleGroup.getToggles().size() - 1));
        }
        if (lightingModeCombo != null) {
            lightingModeCombo.setValue(lightingMode);
        }
    }

    private void applySkinToneToMaterials() {
        Color tone = skinTone.get() != null ? skinTone.get() : DEFAULT_SKIN_TONE;
        for (PhongMaterial material : activeMaterials) {
            material.setDiffuseColor(Color.WHITE);
            material.setSpecularColor(Color.gray(0.05));
            material.setSpecularPower(8);
        }
        tattooWorkspace.updateSkinTone(tone);
    }

    private void syncSkinToneSelection() {
        if (skinToneToggleGroup == null) {
            return;
        }
        Color target = skinTone.get() != null ? skinTone.get() : DEFAULT_SKIN_TONE;
        ToggleButton bestMatch = null;
        double bestDiff = Double.MAX_VALUE;
        for (Toggle toggle : skinToneToggleGroup.getToggles()) {
            Object data = toggle.getUserData();
            if (!(data instanceof Color color)) {
                continue;
            }
            double diff = colorDistanceSq(color, target);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestMatch = (ToggleButton) toggle;
            }
        }
        if (bestMatch != null) {
            skinToneToggleGroup.selectToggle(bestMatch);
        }
    }

    private void applyInitialSkinTone() {
        if (SKIN_TONE_PALETTE.isEmpty()) {
            return;
        }
        Color desired = SKIN_TONE_PALETTE.get(SKIN_TONE_PALETTE.size() - 1);
        if (!Objects.equals(skinTone.get(), desired)) {
            skinTone.set(desired);
        } else {
            tattooWorkspace.updateSkinTone(desired);
            syncSkinToneSelection();
        }
    }

    private static double colorDistanceSq(Color a, Color b) {
        double dr = a.getRed() - b.getRed();
        double dg = a.getGreen() - b.getGreen();
        double db = a.getBlue() - b.getBlue();
        return dr * dr + dg * dg + db * db;
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
            resetDragOffsets();
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
            tattooWorkspace.selected().ifPresent(tattoo -> {
                syncTattooControls(tattoo);
                captureDragOffsets(tattoo, u, v);
            });
            updateTattooControlsState();
            tattooDragActive = true;
            event.consume();
            return;
        }

        if (!tattooWorkspace.hasPendingImage()) {
            tattooWorkspace.clearSelection();
            syncTattooControls(null);
            resetDragOffsets();
            updateTattooControlsState();
            return;
        }

        Tattoo newTattoo = createTattooFromPending(u, v);
        if (newTattoo == null) {
            return;
        }
        tattooWorkspace.addTattoo(newTattoo);
        syncTattooControls(newTattoo);
        resetDragOffsets();
        if (historyPlacementArmed) {
            historyPlacementArmed = false;
            if (tattooHistoryToggleGroup != null) {
                tattooHistoryToggleGroup.selectToggle(null);
            } else {
                tattooWorkspace.clearPendingTattoo();
            }
        }
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
        double u = clamp(uv.getX() - tattooDragOffsetU, 0.0, 1.0);
        double v = clamp(uv.getY() - tattooDragOffsetV, 0.0, 1.0);
        tattooWorkspace.updateSelectedTattoo(current.withUV(u, v));
        event.consume();
    }

    private void handleTattooRelease(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY || !event.isPrimaryButtonDown()) {
            tattooDragActive = false;
            resetDragOffsets();
        }
    }

    private void syncTattooControls(Tattoo tattoo) {
        if (tattoo == null || tattooWidthSlider == null || tattooHeightSlider == null
            || tattooOpacitySlider == null || tattooRotationSlider == null) {
            return;
        }
        adjustingSliders = true;
        try {
            tattooWidthSlider.setValue(tattoo.widthScale());
            tattooHeightSlider.setValue(tattoo.heightScale());
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
        double widthScale = sliderValueOrFallback(tattooWidthSlider, DEFAULT_TATTOO_SCALE);
        double heightScale = sliderValueOrFallback(tattooHeightSlider, DEFAULT_TATTOO_SCALE);
        if (isAspectRatioLocked()) {
            heightScale = widthScale;
        }
        double opacity = sliderValueOrFallback(tattooOpacitySlider, 1.0);
        double rotation = sliderValueOrFallback(tattooRotationSlider, 0.0);
        Image pending = tattooWorkspace.consumePendingImage().orElse(null);
        if (pending == null) {
            return null;
        }
        return new Tattoo(u, v, pending, widthScale, heightScale, rotation, opacity);
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

        currentModelPath = null;
        applyModel(createPlaceholderModel());
    }

    private void loadFromPath(Path path) {
        boolean loaded = false;
        try {
            currentModelPath = path;
            ObjLoader.LoadedModel model = ObjLoader.load(path);
            applyModel(model);
            loaded = true;
        } catch (IOException ex) {
            showError("Failed to load OBJ model: " + path.getFileName(), ex);
            currentModelPath = null;
            applyModel(createPlaceholderModel());
        } finally {
            if (loaded) {
                cleanupExtractionIfNotNeeded(path);
            }
        }
    }

    private void applyModel(ObjLoader.LoadedModel loadedModel) {
        adjustingSliders = true;
        try {
            tattooWorkspace.clearSurface();
            tattooWorkspace.resetMaterials();
            historyPlacementArmed = false;
            if (tattooHistoryToggleGroup != null) {
                tattooHistoryToggleGroup.selectToggle(null);
            }

            partRoot.getTransforms().clear();
            for (Group group : partGroups.values()) {
                group.getChildren().clear();
                group.setScaleX(1.0);
                group.setScaleY(1.0);
                group.setScaleZ(1.0);
            }

            overallScale.set(1.0);

            for (ObjLoader.ModelPart part : loadedModel.parts()) {
                Group targetGroup = resolvePartGroup(part.name());
                targetGroup.getChildren().add(part.node());
            }

            modelHasUVs = loadedModel.parts().stream()
                .map(ObjLoader.ModelPart::node)
                .anyMatch(this::nodeHasUVs);

            if (loadedModel.requiresYAxisFlip()) {
                partRoot.getTransforms().add(new Scale(1, -1, 1));
            }

            if (loadedModel.requiresZUpCorrection()) {
                partRoot.getTransforms().add(new Rotate(-90, Rotate.X_AXIS));
            }

            normalizeModel();
            if (!modelHasUVs) {
                showNoUVMessage();
            }
            initializeTattooPipeline();
            loadTattooMetadataIfPresent();
            updateCurrentBounds();
            cameraRig.reset();
            loadWorkspacePreferencesIfPresent();
            softenSpecular(modelRoot);
            refreshLightingRig();
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

    private void initializeTattooPipeline() {
        modelProvidesBaseTexture = false;
        List<PhongMaterial> materials = new ArrayList<>();
        collectMaterials(modelRoot, materials);
        activeMaterials.clear();
        activeMaterials.addAll(materials);
        initializeSkinToneFromMaterials();
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
            modelProvidesBaseTexture = true;
        }

        double texWidth = Math.max(1.0, baseTexture.getWidth());
        double texHeight = Math.max(1.0, baseTexture.getHeight());
        tattooWorkspace.configureSurface(baseTexture, texWidth, texHeight);
        applyInitialSkinTone();
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

    private boolean nodeHasUVs(Node node) {
        if (node instanceof MeshView mv && mv.getMesh() instanceof TriangleMesh tm) {
            return tm.getTexCoords().size() > 0;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (nodeHasUVs(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateTattooControlsState() {
        boolean placementEnabled = tattooWorkspace.isPlacementAvailable();
        boolean hasSelection = tattooWorkspace.selected().isPresent();
        boolean hasHistory = !tattooHistory.isEmpty();
        boolean slidersEnabled = placementEnabled && hasSelection;
        TattooPreset selectedPreset = selectedHistoryPreset();
        if (loadTattooButton != null) {
            loadTattooButton.setDisable(!modelHasUVs);
        }
        if (tattooWidthSlider != null) {
            tattooWidthSlider.setDisable(!slidersEnabled);
        }
        if (tattooHeightSlider != null) {
            tattooHeightSlider.setDisable(!slidersEnabled);
        }
        if (tattooOpacitySlider != null) {
            tattooOpacitySlider.setDisable(!slidersEnabled);
        }
        if (tattooRotationSlider != null) {
            tattooRotationSlider.setDisable(!slidersEnabled);
        }
        if (lockAspectRatioToggle != null) {
            lockAspectRatioToggle.setDisable(!slidersEnabled);
        }
        if (tattooHistoryScroll != null) {
            tattooHistoryScroll.setDisable(!hasHistory);
        }
        if (deleteTattooButton != null) {
            deleteTattooButton.setDisable(!hasSelection);
        }
        if (removeBackgroundButton != null) {
            removeBackgroundButton.setDisable(selectedPreset == null);
        }
        if (invertTattooButton != null) {
            invertTattooButton.setDisable(selectedPreset == null);
        }
        if (reflectTattooButton != null) {
            reflectTattooButton.setDisable(selectedPreset == null);
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


    private void writeTattooMetadata(
        Path metadataPath,
        Path tattooDir,
        List<Tattoo> tattoos,
        List<TattooPreset> history,
        Path baseTexturePath
    ) throws IOException {
        Properties props = new Properties();
        props.setProperty(VERSION, "2");
        Path baseDir = metadataPath.getParent();
        if (tattooDir != null) {
            Files.createDirectories(tattooDir);
        }
        if (baseTexturePath != null) {
            props.setProperty("baseTexture", relativize(baseDir, baseTexturePath));
        }
        props.setProperty("tattoo.count", Integer.toString(tattoos != null ? tattoos.size() : 0));
        Path appliedDir = tattooDir.resolve("applied");
        Files.createDirectories(appliedDir);
        if (tattoos != null) {
            for (int i = 0; i < tattoos.size(); i++) {
                Tattoo tattoo = tattoos.get(i);
                String prefix = "tattoo." + i;
                props.setProperty(prefix + ".u", Double.toString(tattoo.u()));
                props.setProperty(prefix + ".v", Double.toString(tattoo.v()));
                props.setProperty(prefix + ".widthScale", Double.toString(tattoo.widthScale()));
                props.setProperty(prefix + ".heightScale", Double.toString(tattoo.heightScale()));
                props.setProperty(prefix + DOT_SCALE, Double.toString(tattoo.scale()));
                props.setProperty(prefix + ".rotation", Double.toString(tattoo.rotation()));
                props.setProperty(prefix + ".alpha", Double.toString(tattoo.alpha()));
                Path imagePath = appliedDir.resolve(String.format("tattoo-%02d.png", i + 1));
                writeTattooImage(tattoo.image(), imagePath);
                props.setProperty(prefix + DOT_IMAGE, relativize(baseDir, imagePath));
            }
        }
        List<TattooPreset> historyToPersist = history != null ? history : List.of();
        props.setProperty("history.count", Integer.toString(historyToPersist.size()));
        Path historyDir = tattooDir.resolve("history");
        Files.createDirectories(historyDir);
        for (int i = 0; i < historyToPersist.size(); i++) {
            TattooPreset preset = historyToPersist.get(i);
            String prefix = "history." + i;
            String label = preset.label() != null ? preset.label() : "";
            props.setProperty(prefix + ".label", label);
            Path imagePath = historyDir.resolve(String.format("history-%02d.png", i + 1));
            writeTattooImage(preset.image(), imagePath);
            props.setProperty(prefix + DOT_IMAGE, relativize(baseDir, imagePath));
        }
        if (metadataPath.getParent() != null) {
            Files.createDirectories(metadataPath.getParent());
        }
        try (OutputStream out = Files.newOutputStream(metadataPath)) {
            props.store(out, "Tattui tattoo metadata");
        }
    }

    private Path writeBaseTextureImage(Path target) throws IOException {
        Image base = tattooWorkspace.baseTexture();
        if (base == null) {
            return null;
        }
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        ImageIO.write(convertToBufferedImage(base), "png", target.toFile());
        return target;
    }

    private void writeTattooImage(Image image, Path target) throws IOException {
        if (image == null) {
            return;
        }
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        ImageIO.write(convertToBufferedImage(image), "png", target.toFile());
    }

    private String relativize(Path baseDir, Path target) {
        if (target == null) {
            return "";
        }
        if (baseDir == null) {
            return target.getFileName().toString();
        }
        return baseDir.relativize(target).toString().replace('\\', '/');
    }

    private void loadTattooMetadataIfPresent() {
        if (currentModelPath == null) {
            return;
        }
        Path metadataPath = metadataPathFor(currentModelPath);
        if (!Files.exists(metadataPath)) {
            return;
        }
        Properties props;
        try {
            props = readTattooProperties(metadataPath);
        } catch (IOException ex) {
            showError("Failed to load tattoo metadata", ex);
            return;
        }
        Path baseDir = metadataPath.getParent();
        applyBaseTextureOverride(props, baseDir);
        tattooWorkspace.replaceTattoos(readAppliedTattoos(props, baseDir));
        int version = parseInt(props.getProperty(VERSION), 1);
        List<TattooPreset> history = version >= 2 ? loadTattooHistory(props, baseDir) : List.of();
        restoreTattooHistory(history);
        updateTattooControlsState();
    }

    private Properties readTattooProperties(Path metadataPath) throws IOException {
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(metadataPath)) {
            props.load(input);
        }
        return props;
    }

    private void applyBaseTextureOverride(Properties props, Path baseDir) {
        if (modelProvidesBaseTexture) {
            return;
        }
        Image overrideBase = loadImage(baseDir, props.getProperty("baseTexture"));
        if (overrideBase == null) {
            return;
        }
        double width = Math.max(1.0, overrideBase.getWidth());
        double height = Math.max(1.0, overrideBase.getHeight());
        tattooWorkspace.configureSurface(overrideBase, width, height);
    }

    private List<Tattoo> readAppliedTattoos(Properties props, Path baseDir) {
        int count = parseInt(props.getProperty("tattoo.count"), 0);
        List<Tattoo> restored = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Tattoo tattoo = readTattooEntry(props, baseDir, i);
            if (tattoo != null) {
                restored.add(tattoo);
            }
        }
        return restored;
    }

    private Tattoo readTattooEntry(Properties props, Path baseDir, int index) {
        String prefix = "tattoo." + index;
        Image tattooImage = loadImage(baseDir, props.getProperty(prefix + DOT_IMAGE));
        if (tattooImage == null) {
            return null;
        }
        double u = parseDouble(props.getProperty(prefix + ".u"), 0.5);
        double v = parseDouble(props.getProperty(prefix + ".v"), 0.5);
        double widthScale = resolvedScale(props, prefix + ".widthScale", prefix + DOT_SCALE);
        double heightScale = resolvedScale(props, prefix + ".heightScale", prefix + DOT_SCALE);
        double rotation = parseDouble(props.getProperty(prefix + ".rotation"), 0.0);
        double alpha = parseDouble(props.getProperty(prefix + ".alpha"), 1.0);
        return new Tattoo(u, v, tattooImage, widthScale, heightScale, rotation, alpha);
    }

    private double resolvedScale(Properties props, String scaleKey, String legacyKey) {
        double candidate = parseDouble(props.getProperty(scaleKey), Double.NaN);
        if (Double.isFinite(candidate) && candidate > 0.0) {
            return candidate;
        }
        return parseDouble(props.getProperty(legacyKey), DEFAULT_TATTOO_SCALE);
    }

    private List<TattooPreset> loadTattooHistory(Properties props, Path baseDir) {
        int historyCount = parseInt(props.getProperty("history.count"), 0);
        List<TattooPreset> presets = new ArrayList<>();
        for (int i = 0; i < historyCount && presets.size() < MAX_TATTOO_HISTORY; i++) {
            String prefix = "history." + i;
            Image image = loadImage(baseDir, props.getProperty(prefix + DOT_IMAGE));
            if (image == null) {
                continue;
            }
            String label = props.getProperty(prefix + ".label");
            if (label == null || label.isBlank()) {
                label = "Tattoo " + (i + 1);
            }
            presets.add(new TattooPreset(label, image));
        }
        return presets;
    }

    private void restoreTattooHistory(List<TattooPreset> presets) {
        tattooHistory.clear();
        if (presets != null && !presets.isEmpty()) {
            int limit = Math.min(MAX_TATTOO_HISTORY, presets.size());
            tattooHistory.addAll(presets.subList(0, limit));
        }
        historyPlacementArmed = false;
        tattooWorkspace.clearPendingTattoo();
        if (tattooHistoryToggleGroup != null) {
            tattooHistoryToggleGroup.selectToggle(null);
        }
        refreshTattooHistoryGallery();
    }

    private void loadWorkspacePreferencesIfPresent() {
        if (currentModelPath == null) {
            return;
        }
        Path prefsPath = preferencesPathFor(currentModelPath);
        if (!Files.exists(prefsPath)) {
            return;
        }
        try {
            WorkspacePreferences preferences = readWorkspacePreferences(prefsPath);
            applyWorkspacePreferences(preferences);
        } catch (IOException ex) {
            showError("Failed to load workspace preferences", ex);
        }
    }

    private WorkspacePreferences captureWorkspacePreferences() {
        CameraState camera2 = cameraRig != null ? cameraRig.snapshot() : null;
        Color tone = skinTone.get();
        return new WorkspacePreferences(camera2, tone, lightingMode);
    }

    private void writeWorkspacePreferences(Path preferencesPath, WorkspacePreferences preferences) throws IOException {
        if (preferencesPath == null || preferences == null) {
            return;
        }
        Properties props = new Properties();
        props.setProperty(VERSION, "1");
        if (preferences.camera() != null) {
            CameraState camera2 = preferences.camera();
            props.setProperty("camera.yaw", Double.toString(camera2.yaw()));
            props.setProperty("camera.pitch", Double.toString(camera2.pitch()));
            props.setProperty("camera.distance", Double.toString(camera2.distance()));
            props.setProperty("camera.targetX", Double.toString(camera2.targetX()));
            props.setProperty("camera.targetY", Double.toString(camera2.targetY()));
            props.setProperty("camera.targetZ", Double.toString(camera2.targetZ()));
            props.setProperty("camera.panX", Double.toString(camera2.panX()));
            props.setProperty("camera.panY", Double.toString(camera2.panY()));
            props.setProperty("camera.panZ", Double.toString(camera2.panZ()));
        }
        if (preferences.skinTone() != null) {
            props.setProperty("skinTone", colorToHex(preferences.skinTone()));
        }
        if (preferences.lightingMode() != null) {
            props.setProperty("lightingMode", preferences.lightingMode().name());
        }
        if (preferencesPath.getParent() != null) {
            Files.createDirectories(preferencesPath.getParent());
        }
        try (OutputStream out = Files.newOutputStream(preferencesPath)) {
            props.store(out, "Tattui workspace preferences");
        }
    }

    private WorkspacePreferences readWorkspacePreferences(Path prefsPath) throws IOException {
        if (prefsPath == null || !Files.exists(prefsPath)) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(prefsPath)) {
            props.load(in);
        }
        CameraState cameraState = parseCameraState(props);
        Color tone = parseColor(props.getProperty("skinTone"));
        LightingSystem.Mode mode = parseLightingMode(props.getProperty("lightingMode"));
        return new WorkspacePreferences(cameraState, tone, mode);
    }

    private void applyWorkspacePreferences(WorkspacePreferences preferences) {
        if (preferences == null) {
            return;
        }
        if (preferences.skinTone() != null) {
            skinTone.set(preferences.skinTone());
        }
        if (preferences.lightingMode() != null) {
            lightingMode = preferences.lightingMode();
            lightingSystem.apply(lightingMode);
            if (lightingModeCombo != null) {
                lightingModeCombo.setValue(lightingMode);
            }
        }
        if (preferences.camera() != null) {
            cameraRig.apply(preferences.camera());
        }
    }

    private CameraState parseCameraState(Properties props) {
        if (props == null) {
            return null;
        }
        Double yaw = parseNullableDouble(props.getProperty("camera.yaw"));
        Double pitch = parseNullableDouble(props.getProperty("camera.pitch"));
        Double distance = parseNullableDouble(props.getProperty("camera.distance"));
        Double targetX = parseNullableDouble(props.getProperty("camera.targetX"));
        Double targetY = parseNullableDouble(props.getProperty("camera.targetY"));
        Double targetZ = parseNullableDouble(props.getProperty("camera.targetZ"));
        Double panX = parseNullableDouble(props.getProperty("camera.panX"));
        Double panY = parseNullableDouble(props.getProperty("camera.panY"));
        Double panZ = parseNullableDouble(props.getProperty("camera.panZ"));
        if (yaw == null || pitch == null || distance == null
            || targetX == null || targetY == null || targetZ == null
            || panX == null || panY == null || panZ == null) {
            return null;
        }
        return new CameraState(yaw, pitch, distance, targetX, targetY, targetZ, panX, panY, panZ);
    }

    private Double parseNullableDouble(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String colorToHex(Color color) {
        if (color == null) {
            return null;
        }
        int r = (int) Math.round(color.getRed() * 255.0);
        int g = (int) Math.round(color.getGreen() * 255.0);
        int b = (int) Math.round(color.getBlue() * 255.0);
        int a = (int) Math.round(color.getOpacity() * 255.0);
        return String.format("#%02X%02X%02X%02X", r, g, b, a);
    }

    private Color parseColor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Color.web(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private LightingSystem.Mode parseLightingMode(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return LightingSystem.Mode.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Image loadImage(Path baseDir, String relative) {
        if (relative == null || relative.isBlank()) {
            return null;
        }
        Path resolved = baseDir != null ? baseDir.resolve(relative).normalize() : Paths.get(relative);
        if (!Files.exists(resolved)) {
            return null;
        }
        return new Image(resolved.toUri().toString());
    }

    private Path metadataPathFor(Path objPath) {
        String base = stripExtension(objPath.getFileName().toString());
        return objPath.resolveSibling(base + ".tattoos");
    }

    private Path preferencesPathFor(Path objPath) {
        String base = stripExtension(objPath.getFileName().toString());
        return objPath.resolveSibling(base + ".prefs");
    }

    private String stripExtension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx <= 0) {
            return name;
        }
        return name.substring(0, idx);
    }

    private int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    private BufferedImage convertToBufferedImage(Image image) {
        int width = (int) Math.max(1, Math.round(image.getWidth()));
        int height = (int) Math.max(1, Math.round(image.getHeight()));
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        if (reader == null) {
            return buffered;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return buffered;
    }

    private Image removeBackground(Image source) {
        if (source == null) {
            return null;
        }
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return null;
        }
        int width = (int) Math.max(1, Math.round(source.getWidth()));
        int height = (int) Math.max(1, Math.round(source.getHeight()));
        WritableImage result = new WritableImage(width, height);
        PixelWriter writer = result.getPixelWriter();
        Color background = reader.getColor(0, 0);
        double threshold = 0.18;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color sample = reader.getColor(x, y);
                double diff = Math.sqrt(colorDistanceSq(sample, background));
                if (diff < threshold) {
                    writer.setColor(x, y, new Color(sample.getRed(), sample.getGreen(), sample.getBlue(), 0.0));
                } else {
                    writer.setColor(x, y, sample);
                }
            }
        }
        return result;
    }

    private Image invertImageColors(Image source) {
        if (source == null) {
            return null;
        }
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return null;
        }
        int width = (int) Math.max(1, Math.round(source.getWidth()));
        int height = (int) Math.max(1, Math.round(source.getHeight()));
        WritableImage inverted = new WritableImage(width, height);
        PixelWriter writer = inverted.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                Color negated = new Color(1.0 - color.getRed(), 1.0 - color.getGreen(), 1.0 - color.getBlue(), color.getOpacity());
                writer.setColor(x, y, negated);
            }
        }
        return inverted;
    }

    private Image reflectImageY(Image source) {
        if (source == null) {
            return null;
        }
        PixelReader reader = source.getPixelReader();
        if (reader == null) {
            return null;
        }
        int width = (int) Math.max(1, Math.round(source.getWidth()));
        int height = (int) Math.max(1, Math.round(source.getHeight()));
        WritableImage reflected = new WritableImage(width, height);
        PixelWriter writer = reflected.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(width - 1 - x, y, reader.getColor(x, y));
            }
        }
        return reflected;
    }

    private double clamp(double value, double min, double max) {
        return Math.clamp(value, min, max);
    }

    private record WorkspacePreferences(CameraState camera, Color skinTone, LightingSystem.Mode lightingMode) {}

    private record CopiedMaterial(Path source, Path target) {}

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
