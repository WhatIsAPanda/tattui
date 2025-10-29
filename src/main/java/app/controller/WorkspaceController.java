package app.controller;

import app.entity.ModelManipulator;
import app.entity.Tattoo;
import app.utils.ObjLoader;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
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
import javafx.scene.layout.Priority;
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
import javafx.scene.shape.DrawMode;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Application controller that encapsulates the workspace behaviour and state.
 */
public class WorkspaceController {
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
    private static final Path DEFAULT_MODEL_DEV_PATH = Paths.get("src", "main", "resources", "models", "human.obj");

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

    // --- Lighting presets ---
    private enum LightingPreset { TOP_DOWN, UNIFORM }

    private final Group lightsLayer = new Group();
    private LightingPreset lightingPreset = LightingPreset.TOP_DOWN;

    private final Group root3D = new Group(modelRoot);

    @FXML
    private BorderPane rootPane;
    @FXML
    private HBox toolbarBox;
    @FXML
    private Button loadModelButton;
    @FXML
    private Button resetViewButton;
    @FXML
    private ComboBox<LightingPreset> lightingCombo;
    @FXML
    private ScrollPane controlScroll;
    @FXML
    private VBox controlsContainer;
    @FXML
    private StackPane viewerPane;

    private boolean bootstrapped;

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

    public WorkspaceController() {
        initializePartGroups();
        configureCamera();
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
            resetViewButton.setOnAction(evt -> resetView());
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

    // private void setupControlPanel() {
    //     if (controlsContainer == null) {
    //         return;
    //     }

    //     controlsContainer.getChildren().clear();
    //     controlsContainer.setSpacing(12);
    //     controlsContainer.setPadding(new Insets(16));
    //     controlsContainer.setFillWidth(true);

    //     proportionControls.clear();
    //     for (String label : PROPORTION_KEYS) {
    //         Slider slider = createSlider(SLIDER_MIN, SLIDER_MAX);
    //         slider.valueProperty().addListener((obs, oldVal, newVal) -> {
    //             if (adjustingSliders) return;
    //             applyCurrentProportions();
    //         });
    //         proportionControls.put(label, slider);

    //         TextField valueField = new TextField(String.format("%.2f", slider.getValue()));
    //         valueField.setPrefWidth(60);
    //         // Allow manual edits
    //         valueField.textProperty().addListener((obs, oldVal, newVal) -> {
    //             try {
    //                 double val = Double.parseDouble(newVal);
    //                 if (val >= SLIDER_MIN && val <= SLIDER_MAX) {
    //                     slider.setValue(val);
    //                 }
    //             } catch (NumberFormatException ignored) {}
    //         });

    //         // Sync field with slider
    //         slider.valueProperty().addListener((obs, oldVal, newVal) ->
    //             valueField.setText(String.format("%.2f", newVal.doubleValue()))
    //         );

    //         HBox hbox = new HBox(8);
    //         hbox.setAlignment(Pos.CENTER_LEFT);
    //         hbox.setFillHeight(true);
    //         HBox.setHgrow(slider, Priority.ALWAYS);

    //         Label nameLabel = new Label(label);
    //         nameLabel.setPrefWidth(120); // consistent label width
    //         hbox.getChildren().addAll(nameLabel, slider, valueField);

    //         controlsContainer.getChildren().add(hbox);
    //     }

    //     Slider overallSlider = createSlider(OVERALL_MIN, OVERALL_MAX);
    //     TextField overallField = new TextField(String.format("%.2f", overallSlider.getValue()));
    //     overallField.setPrefWidth(60);
    //     overallField.setAlignment(Pos.CENTER_RIGHT);
    //     overallField.textProperty().addListener((obs, oldVal, newVal) -> {
    //         try {
    //             double val = Double.parseDouble(newVal);
    //             if (val >= OVERALL_MIN && val <= OVERALL_MAX)
    //                 overallSlider.setValue(val);
    //         } catch (NumberFormatException ignored) {}
    //     });
    //     overallSlider.valueProperty().addListener((obs, o, n) ->
    //         overallField.setText(String.format("%.2f", n.doubleValue()))
    //     );

    //     HBox overallBox = new HBox(10);
    //     overallBox.setAlignment(Pos.CENTER_LEFT);
    //     overallBox.setFillHeight(true);
    //     overallBox.setMaxWidth(Double.MAX_VALUE);

    //     Label overallLabel = new Label("Overall Scale");
    //     overallLabel.setPrefWidth(140);
    //     overallLabel.setMinWidth(140);
    //     overallLabel.setAlignment(Pos.CENTER_LEFT);

    //     HBox.setHgrow(overallSlider, Priority.ALWAYS);

    //     overallBox.getChildren().addAll(overallLabel, overallSlider, overallField);
    //     controlsContainer.getChildren().add(overallBox);


    //     Label tattooLabel = new Label("Tattoo Tools");
    //     loadTattooButton = new Button("Load Tattoo");
    //     loadTattooButton.setOnAction(e -> handleLoadTattoo());

    //     tattooSizeSlider = new Slider(0.05, 1.0, 0.20);
    //     tattooSizeSlider.getStyleClass().add("workspace-slider");
    //     tattooSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
    //         if (selectedTattoo != null) {
    //             selectedTattoo.scale = newVal.doubleValue();
    //             repaintTattooTexture();
    //         }
    //     });

    //     tattooOpacitySlider = new Slider(0.3, 1.0, 1.0);
    //     tattooOpacitySlider.getStyleClass().add("workspace-slider");
    //     tattooOpacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
    //         if (selectedTattoo != null) {
    //             selectedTattoo.alpha = newVal.doubleValue();
    //             repaintTattooTexture();
    //         }
    //     });

    //     tattooRotationSlider = new Slider(-180.0, 180.0, 0.0);
    //     tattooRotationSlider.getStyleClass().add("workspace-slider");
    //     tattooRotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
    //         if (selectedTattoo != null) {
    //             selectedTattoo.rotation = newVal.doubleValue();
    //             repaintTattooTexture();
    //         }
    //     });

    //     VBox tattooControls = new VBox(6,
    //         tattooLabel,
    //         loadTattooButton,
    //         createLabeledControl("Tattoo Size", tattooSizeSlider),
    //         createLabeledControl("Tattoo Opacity", tattooOpacitySlider),
    //         createLabeledControl("Tattoo Rotation", tattooRotationSlider)
    //     );
    //     tattooControls.setFillWidth(true);

    //     controlsContainer.getChildren().add(tattooControls);

    //     if (controlScroll != null) {
    //         controlScroll.setFitToWidth(true);
    //         controlScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    //         controlScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    //         controlScroll.setPadding(Insets.EMPTY);
    //         if (rootPane != null) {
    //             controlScroll.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.35));
    //             controlScroll.maxWidthProperty().bind(rootPane.widthProperty().multiply(0.35));
    //         }
    //     }

    //     updateTattooControlsState();
    // }

    private void setupControlPanel() {
        if (controlsContainer == null) return;

        controlsContainer.getChildren().clear();
        controlsContainer.setSpacing(12);
        controlsContainer.setPadding(new Insets(16));
        controlsContainer.setFillWidth(true);

        // ===== Body Proportions Section =====
        VBox proportionBox = new VBox(8);
        proportionBox.setFillWidth(true);
        proportionControls.clear();

        for (String label : PROPORTION_KEYS) {
            Slider slider = createSlider(SLIDER_MIN, SLIDER_MAX);
            slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (adjustingSliders) return;
                applyCurrentProportions();
            });
            proportionControls.put(label, slider);

            TextField valueField = new TextField(String.format("%.2f", slider.getValue()));
            valueField.setPrefWidth(60);
            valueField.setAlignment(Pos.CENTER_RIGHT);

            // manual edit → update slider
            valueField.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    double val = Double.parseDouble(newVal);
                    if (val >= SLIDER_MIN && val <= SLIDER_MAX)
                        slider.setValue(val);
                } catch (NumberFormatException ignored) {}
            });

            // slider → update field
            slider.valueProperty().addListener((obs, o, n) ->
                valueField.setText(String.format("%.2f", n.doubleValue()))
            );

            Label nameLabel = new Label(label);
            nameLabel.setPrefWidth(140);

            HBox row = new HBox(10, nameLabel, slider, valueField);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(slider, Priority.ALWAYS);
            proportionBox.getChildren().add(row);
        }

        // overall scale row
        Slider overallSlider = createSlider(OVERALL_MIN, OVERALL_MAX);
        TextField overallField = new TextField(String.format("%.2f", overallSlider.getValue()));
        overallField.setPrefWidth(60);
        overallField.setAlignment(Pos.CENTER_RIGHT);

        overallField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double val = Double.parseDouble(newVal);
                if (val >= OVERALL_MIN && val <= OVERALL_MAX)
                    overallSlider.setValue(val);
            } catch (NumberFormatException ignored) {}
        });
        overallSlider.valueProperty().addListener((obs, o, n) ->
            overallField.setText(String.format("%.2f", n.doubleValue()))
        );

        Label overallLabel = new Label("Overall Scale");
        overallLabel.setPrefWidth(140);
        HBox overallRow = new HBox(10, overallLabel, overallSlider, overallField);
        overallRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(overallSlider, Priority.ALWAYS);
        proportionBox.getChildren().add(overallRow);

        TitledPane proportionPane = new TitledPane("Body Proportions", proportionBox);
        proportionPane.setExpanded(false); // collapsed by default


        // ===== Tattoo Settings Section =====
        VBox tattooBox = new VBox(8);
        tattooBox.setFillWidth(true);

        Label tattooLabel = new Label("Tattoo Tools");
        loadTattooButton = new Button("Load Tattoo");
        loadTattooButton.setOnAction(e -> handleLoadTattoo());

        tattooSizeSlider = new Slider(0.05, 1.0, 0.20);
        tattooSizeSlider.getStyleClass().add("workspace-slider");
        tattooSizeSlider.valueProperty().addListener((obs, o, n) -> {
            if (selectedTattoo != null) {
                selectedTattoo.scale = n.doubleValue();
                repaintTattooTexture();
            }
        });

        tattooOpacitySlider = new Slider(0.3, 1.0, 1.0);
        tattooOpacitySlider.getStyleClass().add("workspace-slider");
        tattooOpacitySlider.valueProperty().addListener((obs, o, n) -> {
            if (selectedTattoo != null) {
                selectedTattoo.alpha = n.doubleValue();
                repaintTattooTexture();
            }
        });

        tattooRotationSlider = new Slider(-180.0, 180.0, 0.0);
        tattooRotationSlider.getStyleClass().add("workspace-slider");
        tattooRotationSlider.valueProperty().addListener((obs, o, n) -> {
            if (selectedTattoo != null) {
                selectedTattoo.rotation = n.doubleValue();
                repaintTattooTexture();
            }
        });

        tattooBox.getChildren().addAll(
            tattooLabel,
            loadTattooButton,
            createLabeledControl("Tattoo Size", tattooSizeSlider),
            createLabeledControl("Tattoo Opacity", tattooOpacitySlider),
            createLabeledControl("Tattoo Rotation", tattooRotationSlider)
        );

        TitledPane tattooPane = new TitledPane("Tattoo Settings", tattooBox);
        tattooPane.setExpanded(true); // expanded by default


        // ===== Combine Sections =====
        VBox sectionContainer = new VBox(12, tattooPane, proportionPane);
        sectionContainer.setFillWidth(true);

        controlsContainer.getChildren().add(sectionContainer);

        if (controlScroll != null) {
            controlScroll.setContent(controlsContainer);
            controlScroll.setFitToWidth(true);
            controlScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            controlScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        }

        updateTattooControlsState();
    }


    private Slider createSlider(double min, double max) {
        Slider slider = new Slider(min, max, 1.0);
        slider.getStyleClass().add("workspace-slider");
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

    private void handleLoadTattoo() {
        if (!modelHasUVs || skinCanvas == null) {
            showNoUVMessage();
            return;
        }
        Stage targetStage = resolveStage();
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

    private void configureCamera() {
        camera.setNearClip(0.1);
        camera.setFarClip(20000);
        camera.setFieldOfView(35);

        yaw.addListener((obs, oldV, newV) -> updateCameraTransform());
        pitch.addListener((obs, oldV, newV) -> updateCameraTransform());
        distance.addListener((obs, oldV, newV) -> updateCameraTransform());

        updateCameraTransform();
    }

    private void initLightingLayer(Group sceneRoot) {
        if (!sceneRoot.getChildren().contains(lightsLayer)) {
            sceneRoot.getChildren().add(lightsLayer); // sibling of the model, no transforms
        }
    }

    private AmbientLight amb(double g) {
        return new AmbientLight(Color.gray(g));
    }

    private DirectionalLight dir(double g, double x, double y, double z) {
        DirectionalLight light = new DirectionalLight(Color.gray(g));
        light.setDirection(new Point3D(x, y, z).normalize());
        return light;
    }

    private void applyLighting(LightingPreset preset) {
        initLightingLayer(root3D);
        lightsLayer.getChildren().clear();

        switch (preset) {
            case TOP_DOWN -> lightsLayer.getChildren().addAll(
                amb(0.25),               // soft fill
                dir(1.0, 0, -1, 0)       // straight down
            );
            case UNIFORM -> lightsLayer.getChildren().addAll(
                amb(0.70),
                dir(0.35, -0.35, -1, -0.35), // front-left, slightly above
                dir(0.25, 0.35, -1, 0.35)    // back-right, slightly above
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
                shape.setCullFace(CullFace.BACK); // prevent double-lit backfaces
                shape.setDrawMode(DrawMode.FILL);
            }
        });
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
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double dx = event.getSceneX() - lastMouseX;
            double dy = event.getSceneY() - lastMouseY;
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();

            if (event.isControlDown()) {
                // Ctrl + Drag → rotate model
                orbit(dx, dy);
            } else {
                // Regular Drag → move tattoo
                if (selectedTattoo != null && modelHasUVs && skinCanvas != null) {
                    handleTattooDrag(event);
                } else {
                    // fallback to pan if no tattoo selected
                    pan(dx, dy);
                }
            }
        });

        subScene.addEventHandler(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;
            double delta = event.getDeltaY() * ZOOM_SENSITIVITY;
            distance.set(clamp(distance.get() - delta, MIN_DISTANCE, MAX_DISTANCE));
        });

        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.MIDDLE || 
                (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)) {
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

        java.io.File file = chooser.showOpenDialog(targetStage);
        if (file != null) {
            loadFromPath(file.toPath());
        }
    }

    public void loadInitialModel() {
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

}
