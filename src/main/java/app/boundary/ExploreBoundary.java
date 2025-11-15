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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ExploreBoundary implements RootController.WorkspaceAware, RootController.PageAware {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterBox;
    @FXML private TilePane resultsPane;

    private Supplier<WorkspaceController> workspaceProvider;
    private Consumer<String> pageRequest;

    // Pure logic lives here
    private final ExploreControl control = new ExploreControl();

    @Override
    public void setWorkspaceProvider(Supplier<WorkspaceController> provider) {
        this.workspaceProvider = provider;
        System.out.println("[ExploreBoundary] workspaceProvider injected? " + (provider != null));
    }

    @Override
    public void setOnPageRequest(Consumer<String> handler) {
        this.pageRequest = handler;
    }

    @FXML
    private void initialize() {
        System.out.println("[ExploreBoundary] init: searchField=" + searchField
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

        List<ExploreControl.SearchItem> items = control.filter(q, kind);

        resultsPane.getChildren().setAll(
                items.isEmpty()
                        ? List.of(new Label("No results. Try a different search or filter."))
                        : items.stream().map(this::card).toList()
        );

        System.out.println("[ExploreBoundary] results=" + items.size()
                + " filter=" + filterBox.getSelectionModel().getSelectedItem()
                + " q=\"" + q + "\"");
    }

    private Node card(ExploreControl.SearchItem item) {
        ImageView iv = new ImageView();
        var res = getClass().getResourceAsStream(item.thumbnail());
        if (res != null) {
            iv.setImage(new Image(res, 220, 0, true, true));
            iv.setFitWidth(220);
            iv.setPreserveRatio(true);
        }

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
                    var SAVE   = new ButtonType("Save Locally");
                    var SEND   = new ButtonType("Send to Workspace");
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
                case COMPLETED_TATTOOS -> enlargeImage(iv.getImage(), item.title());
                case ARTISTS -> openArtistPage(item.title());
                default -> System.out.println("[ExploreBoundary] clicked " + item.title());
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
            if (pageRequest != null) pageRequest.accept("workspace");
        } else {
            new Alert(Alert.AlertType.WARNING,
                    "Workspace didn’t accept the image.\nMake sure placement is initialized.").showAndWait();
        }
    }

    private void saveImageToLocal(Image img, String name) {
        try {
            if (img == null) return;
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
        if (img == null) return;
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
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/app/view/ArtistProfile.fxml"));
            javafx.scene.Parent root = loader.load();   // declare type explicitly
            var controller = loader.getController();


            String photo = "/icons/artist_raven.jpg";
            String bio = """
                    Raven (she/her) is a punk-studio tattoo artist specializing in bold blackwork,
                    geometric abstractions, and negative-space composition. She loves sleeves and
                    large-scale projects that flow with the body.
                    """;

            controller.getClass().getMethod("setData", String.class, String.class, String.class)
                    .invoke(controller, artistName, bio, photo);

            var stage = new javafx.stage.Stage();
            stage.setTitle("Artist Profile: " + artistName);
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
