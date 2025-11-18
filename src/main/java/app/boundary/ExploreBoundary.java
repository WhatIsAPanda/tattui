package app.boundary;

import app.controller.RootController;
import app.controller.WorkspaceController;
import app.controller.explore.ExploreControl;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ExploreBoundary implements RootController.WorkspaceAware, RootController.PageAware, RootController.ProfileAware {

    // ---- lightweight debug helper ----
    private static final boolean DEBUG = Boolean.getBoolean("TATTUI_DEBUG");
    private static final String DEFAULT_ARTIST_PHOTO = "/icons/artist_raven.jpg";
    private static final String DEFAULT_BIO = "No biography yet.";
    private static final String ARTIST_PROFILE_FXML = "/app/view/ArtistProfile.fxml";

    private static void dbg(String msg) {
        if (DEBUG)
            System.out.println(msg);
    }

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterBox;
    @FXML
    private TilePane resultsPane;

    private Supplier<WorkspaceController> workspaceProvider;
    private Consumer<String> pageRequest;
    private Consumer<app.entity.Profile> profileRequest;

    // Pure logic lives here
    private final ExploreControl control = new ExploreControl();

    // ---- Explore provider selection ----
// Priority:
// 1) EXPLORE_MOCK => force mock
// 2) EXPLORE_LIVE => force live
// 3) Else: try live; if DB not configured/reachable, fall back to mock.
    private final app.controller.explore.ExploreDataProvider provider = selectProvider();

    private app.controller.explore.ExploreDataProvider selectProvider() {
        try {
            if (System.getenv("EXPLORE_MOCK") != null) {
                dbg("[ExploreBoundary] Mode=MOCK (forced by EXPLORE_MOCK)");
                return new app.controller.explore.MockExploreDataProvider();
            }
            if (System.getenv("EXPLORE_LIVE") != null) {
                dbg("[ExploreBoundary] Mode=LIVE (forced by EXPLORE_LIVE)");
                return new app.controller.explore.MergedExploreDataProvider();
            }
            // Default: prefer LIVE if DB is reachable
            if (canConnectToDb()) {
                dbg("[ExploreBoundary] Mode=LIVE (default; DB reachable)");
                return new app.controller.explore.MergedExploreDataProvider();
            } else {
                dbg("[ExploreBoundary] Mode=MOCK (fallback; DB not reachable)");
                return new app.controller.explore.MockExploreDataProvider();
            }
        } catch (Exception e) {
            e.printStackTrace();
            dbg("[ExploreBoundary] Mode=MOCK (fallback due to exception)");
            return new app.controller.explore.MockExploreDataProvider();
        }
    }

    // Tiny probe: attempt to open/close a DB connection.
// Uses DbConnectionProvider to share the same credential paths teammates already use.
    private boolean canConnectToDb() {
        try (java.sql.Connection c = app.db.DbConnectionProvider.open()) {
            return c != null && !c.isClosed();
        } catch (Exception ignored) {
            return false;
        }
    }


    @Override
    public void setWorkspaceProvider(Supplier<WorkspaceController> provider) {
        this.workspaceProvider = provider;
        dbg("[ExploreBoundary] workspaceProvider injected? " + (provider != null));
    }

    @Override
    public void setOnPageRequest(Consumer<String> handler) {
        this.pageRequest = handler;
    }

    @Override
    public void setProfileProvider(Consumer<app.entity.Profile> provider) {
        this.profileRequest = provider;
    }

    @FXML
    private void initialize() {
        dbg("[ExploreBoundary] init: searchField=" + searchField
                + ", filterBox=" + filterBox + ", resultsPane=" + resultsPane);

        // Filters
        filterBox.getItems().setAll("All", "Artists", "Designs", "Completed Tattoos");
        filterBox.getSelectionModel().selectFirst();

        // Listeners
        searchField.textProperty().addListener((o, a, b) -> refreshResults());
        filterBox.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshResults());

        // First draw
        refreshResults();
    }

    private void refreshResults() {
        if (filterBox == null || searchField == null || resultsPane == null) {
            return;
        }
        String q = (searchField.getText() == null ? "" : searchField.getText())
                .trim().toLowerCase(Locale.ROOT);
        ExploreControl.Kind kind = switch (filterBox.getSelectionModel().getSelectedItem()) {
            case "Artists" -> ExploreControl.Kind.ARTISTS;
            case "Designs" -> ExploreControl.Kind.DESIGNS;
            case "Completed Tattoos" -> ExploreControl.Kind.COMPLETED_TATTOOS;
            default -> ExploreControl.Kind.ALL;
        };

        List<ExploreControl.SearchItem> items = provider.fetch(q, kind);

        resultsPane.getChildren().setAll(
                items.isEmpty()
                        ? List.of(new Label("No results. Try a different search or filter."))
                        : items.stream().map(this::card).toList());

        dbg("[ExploreBoundary] results=" + items.size()
                + " filter=" + filterBox.getSelectionModel().getSelectedItem()
                + " q=\"" + q + "\"");
    }

    private Node card(ExploreControl.SearchItem item) {
        ImageView iv = new ImageView();
        String thumb = item.thumbnail();

        if (thumb != null && (thumb.startsWith("http://") || thumb.startsWith("https://"))) {
            iv.setImage(new Image(thumb, 220, 0, true, true)); // Cloudinary URL
        } else {
            var res = getClass().getResourceAsStream(thumb);
            if (res != null)
                iv.setImage(new Image(res, 220, 0, true, true));
        }
        iv.setFitWidth(220);
        iv.setPreserveRatio(true);

        Label overlay = new Label(item.hoverText());
        overlay.setWrapText(true);
        overlay.setStyle("""
                -fx-background-color: rgba(0,0,0,0.7);
                -fx-text-fill: white;
                -fx-padding: 8;
                -fx-font-size: 12px;
                -fx-background-radius: 6;
                -fx-opacity: 0;
                -fx-alignment: center;
                """);
        overlay.setMaxWidth(220);

        StackPane imageStack = new StackPane(iv, overlay);

        Label title = new Label(item.title());
        Label kind = new Label(switch (item.kind()) {
            case ARTISTS -> "Artist";
            case DESIGNS -> "Design";
            case COMPLETED_TATTOOS -> "Completed Tattoo";
            default -> "";
        });
        Label tags = new Label(String.join(" • ", item.tags()));
        tags.setWrapText(true);

        VBox box = new VBox(8, imageStack, title, kind, tags);
        box.setPrefWidth(240);
        box.setMinWidth(240);
        box.setMaxWidth(240);
        box.setStyle("-fx-padding:10; -fx-background-radius:12; -fx-background-color: rgba(255,255,255,0.05);");

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlay);
        fadeIn.setToValue(1.0);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlay);
        fadeOut.setToValue(0.0);

        box.setOnMouseEntered(e -> {
            box.setScaleX(1.05);
            box.setScaleY(1.05);
            fadeIn.playFromStart();
        });
        box.setOnMouseExited(e -> {
            box.setScaleX(1.0);
            box.setScaleY(1.0);
            fadeOut.playFromStart();
        });

        box.setOnMouseClicked(e -> {
            switch (item.kind()) {
                case DESIGNS -> {
                    var SAVE = new ButtonType("Save Locally");
                    var SEND = new ButtonType("Send to Workspace");
                    var CANCEL = ButtonType.CANCEL;

                    var alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setHeaderText("What do you want to do?");
                    alert.setContentText("Design: " + item.title());
                    alert.getButtonTypes().setAll(SEND, SAVE, CANCEL);

                    var choice = alert.showAndWait().orElse(CANCEL);
                    if (choice == SAVE) {
                        saveImageToLocal(iv.getImage(), item.title());
                    } else if (choice == SEND) {
                        exportToWorkspace(iv.getImage(), item.title());
                    }
                }
                case COMPLETED_TATTOOS -> showCompletedTattooModal(item, iv.getImage());
                case ARTISTS -> openArtistPage(item.title());
                default -> dbg("[ExploreBoundary] clicked " + item.title());
            }
        });

        return box;
    }

    // ------- helpers (UI-side) -------

    private void exportToWorkspace(Image img, String title) {
        if (img == null) {
            new Alert(Alert.AlertType.WARNING, "No image available to send.").showAndWait();
            return;
        }
        if (workspaceProvider == null || workspaceProvider.get() == null) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Workspace is not available yet.\nOpen Workspace once, then try again.").showAndWait();
            return;
        }

        boolean accepted = false;
        try {
            accepted = workspaceProvider.get().openTattooFromGallery(img, title);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (accepted) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Sent \"" + title + "\" to the Workspace.").showAndWait();
            if (pageRequest != null)
                pageRequest.accept("workspace");
        } else {
            new Alert(Alert.AlertType.WARNING,
                    "Workspace didn’t accept the image.\nMake sure placement is initialized.").showAndWait();
        }
    }

    private void saveImageToLocal(Image img, String name) {
        try {
            if (img == null)
                return;
            File file = new File(System.getProperty("user.home")
                    + "/Downloads/" + name.replaceAll("\\s+", "_") + ".png");
            javax.imageio.ImageIO.write(
                    javafx.embed.swing.SwingFXUtils.fromFXImage(img, null), "png", file);
            new Alert(Alert.AlertType.INFORMATION,
                    "Saved to:\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Save failed for \"" + name + "\".").showAndWait();
        }
    }

    private void enlargeImage(Image img, String title) {
        if (img == null)
            return;
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setFitWidth(600);
        var scene = new javafx.scene.Scene(new StackPane(iv), 700, 700);
        var stage = new javafx.stage.Stage();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }

    private void openArtistPage(String artistName) {
        app.entity.Profile profile = fetchProfile(artistName);
        if (profileRequest != null && profile != null) {
            profileRequest.accept(profile);
            return;
        }

        String photo = resolvePhoto(profile);
        String bio = resolveBio(profile);

        if (showFXMLProfile(artistName, bio, photo)) {
            return;
        }

        showFallbackProfile(artistName, bio, photo);
    }

    private app.entity.Profile fetchProfile(String artistName) {
        try {
            return app.entity.DatabaseConnector.getProfileByUsername(artistName);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String resolvePhoto(app.entity.Profile profile) {
        if (profile != null && profile.getProfilePictureURL() != null && !profile.getProfilePictureURL().isBlank()) {
            return profile.getProfilePictureURL();
        }
        return DEFAULT_ARTIST_PHOTO;
    }

    private String resolveBio(app.entity.Profile profile) {
        if (profile != null && profile.biography != null && !profile.biography.isBlank()) {
            return profile.biography;
        }
        return DEFAULT_BIO;
    }

    private boolean showFXMLProfile(String artistName, String bio, String photo) {
        try {
            var fxml = getClass().getResource(ARTIST_PROFILE_FXML);
            if (fxml == null) {
                return false;
            }
            var loader = new javafx.fxml.FXMLLoader(fxml);
            javafx.scene.Parent root = loader.load();
            Object controller = loader.getController();
            if (controller == null) {
                return false;
            }

            boolean invoked = tryInvokeSetData(controller, artistName, bio, photo)
                    || tryInvokeSetData(controller, artistName, photo, bio);
            if (!invoked) {
                return false;
            }

            var stage = new javafx.stage.Stage();
            stage.setTitle("Artist Profile: " + artistName);
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
            return true;
        } catch (Exception fx) {
            fx.printStackTrace();
            return false;
        }
    }

    private boolean tryInvokeSetData(Object controller, String first, String second, String third) {
        try {
            controller.getClass().getMethod("setData", String.class, String.class, String.class)
                    .invoke(controller, first, second, third);
            return true;
        } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            return false;
        }
    }

    private void showFallbackProfile(String artistName, String bio, String photo) {
        try {
            javafx.scene.image.ImageView avatar = new javafx.scene.image.ImageView();
            applyAvatarImage(avatar, photo);

            var nameLbl = new javafx.scene.control.Label(artistName);
            nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
            var bioLbl = new javafx.scene.control.Label(bio);
            bioLbl.setWrapText(true);
            bioLbl.setStyle("-fx-text-fill: #ddd;");

            var box = new javafx.scene.layout.VBox(12, avatar, nameLbl, bioLbl);
            box.setStyle("-fx-padding: 16; -fx-background-color: #222; -fx-text-fill: white;");

            var scene = new javafx.scene.Scene(box, 420, 360);
            var stage = new javafx.stage.Stage();
            stage.setTitle(artistName);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Could not open profile for " + artistName).showAndWait();
        }
    }

    private void applyAvatarImage(javafx.scene.image.ImageView avatar, String photo) {
        try {
            if (photo.startsWith("http://") || photo.startsWith("https://")) {
                avatar.setImage(new javafx.scene.image.Image(photo, 160, 160, true, true));
            } else {
                var in = getClass().getResourceAsStream(photo);
                if (in != null) {
                    avatar.setImage(new javafx.scene.image.Image(in, 160, 160, true, true));
                }
            }
        } catch (Exception ignored) {
            // leave as-is
        }
        avatar.setFitWidth(160);
        avatar.setFitHeight(160);
        avatar.setPreserveRatio(true);
    }

    /**
     * Enlarged view for completed tattoos with caption (bottom) + square avatar
     * (top-right).
     */
    private void showCompletedTattooModal(ExploreControl.SearchItem item, Image baseImage) {
        if (baseImage == null)
            return;

        // Big image
        ImageView big = new ImageView(baseImage);
        big.setPreserveRatio(true);
        big.setFitWidth(1000); // adjust to taste, window is 1100x760 below

        // ----- caption (bottom) -----
        Label caption = new Label(item.hoverText());
        caption.setWrapText(true);
        caption.setAlignment(javafx.geometry.Pos.CENTER);
        caption.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        caption.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-line-spacing: 2px;");

        StackPane captionBar = new StackPane(caption);
        captionBar.setStyle("""
                    -fx-background-color: rgba(0,0,0,0.72);
                    -fx-padding: 12;
                    -fx-background-radius: 0 0 12 12;
                """);
        // IMPORTANT: keep it only as tall as its content (prevents the gray wash across
        // the whole image)
        captionBar.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        // position + width constraint
        caption.setMaxWidth(1000 - 2 * 20); // scene width minus margins
        captionBar.setMaxWidth(1000 - 2 * 20);
        StackPane.setAlignment(captionBar, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setMargin(captionBar, new javafx.geometry.Insets(0, 20, 20, 20));

        // ----- avatar (top-right, SQUARE) -----
        String artistName = parseArtistTag(item.tags()).orElse("Unknown");
        ImageView avatar = new ImageView();
        try {
            app.entity.Profile p = app.entity.DatabaseConnector.getProfileByUsername(artistName);
            if (p != null && p.getProfilePictureURL() != null && !p.getProfilePictureURL().isBlank()) {
                avatar.setImage(new Image(p.getProfilePictureURL(), 56, 56, true, true));
            } else {
                var ares = getClass().getResourceAsStream(DEFAULT_ARTIST_PHOTO);
                if (ares != null)
                    avatar.setImage(new Image(ares, 56, 56, true, true));
            }
        } catch (Exception ignored) {
            var ares = getClass().getResourceAsStream(DEFAULT_ARTIST_PHOTO);
            if (ares != null)
                avatar.setImage(new Image(ares, 56, 56, true, true));
        }
        avatar.setFitWidth(56);
        avatar.setFitHeight(56);
        avatar.setPreserveRatio(true);
        StackPane.setAlignment(avatar, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(avatar, new javafx.geometry.Insets(20, 20, 0, 0));
        avatar.setOnMouseClicked(e -> {
            e.consume();
            openArtistPage(artistName);
        });

        // Root: image + overlays
        StackPane root = new StackPane(big, captionBar, avatar);
        root.setStyle("-fx-background-color: black; -fx-padding: 10;");

        // Scene + Stage (slightly wider than image to allow margins)
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 1100, 760);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle(item.title());
        stage.setScene(scene);
        stage.show();
    }

    private java.util.Optional<String> parseArtistTag(java.util.List<String> tags) {
        return tags.stream()
                .filter(t -> t.toLowerCase(java.util.Locale.ROOT).startsWith("artist:"))
                .map(t -> t.substring("artist:".length()).trim())
                .findFirst();
    }

    private String avatarForArtist(String artistName) {
        if ("Raven".equalsIgnoreCase(artistName))
            return DEFAULT_ARTIST_PHOTO;
        return DEFAULT_ARTIST_PHOTO; // fallback
    }

}
