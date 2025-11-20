package app.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.scene.layout.StackPane;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.boundary.ExploreBoundary;

public class ExploreController implements RootController.WorkspaceAware, RootController.PageAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExploreController.class);

    private java.util.function.Supplier<WorkspaceController> workspaceProvider;
    private java.util.function.Consumer<String> pageRequest;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterBox;
    @FXML
    private TilePane resultsPane;

    private enum Kind {
        ALL, ARTISTS, DESIGNS, COMPLETED_TATTOOS
    }

    private static class SearchItem {
        final String title;
        final Kind kind;
        final String thumbnail;
        final List<String> tags;
        final String hoverText; // new text shown when hovering

        SearchItem(String title, Kind kind, String thumbnail, List<String> tags, String hoverText) {
            this.title = title;
            this.kind = kind;
            this.thumbnail = thumbnail;
            this.tags = tags;
            this.hoverText = hoverText;
        }
    }

    @Override
    public void setWorkspaceProvider(java.util.function.Supplier<WorkspaceController> provider) {
        this.workspaceProvider = provider;
        LOGGER.info("[Explore] workspaceProvider injected? " + (provider != null));
    }

    private void exportToWorkspace(Image img, String title) {
        if (img == null) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING,
                    "No image available to send.").showAndWait();
            return;
        }
        if (workspaceProvider == null || workspaceProvider.get() == null) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Workspace is not available yet.\nOpen the app via Root and/or visit the Workspace page, then try again.")
                    .showAndWait();
            return;
        }

        boolean accepted = false;
        try {
            accepted = workspaceProvider.get().openTattooFromGallery(img, title);
        } catch (Exception _) {
            // workspace consumer rejected image; fall back to UI alerts below
        }

        if (accepted) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Sent \"" + title + "\" to the Workspace.").showAndWait();
            if (pageRequest != null)
                pageRequest.accept("workspace"); // 👈 jump
        } else {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING,
                    "Workspace didn’t accept the image.\nMake sure placement is initialized.").showAndWait();
        }
    }

    /// ////////

    private List<SearchItem> allItems;

    // ?

    @Override
    public void setOnPageRequest(java.util.function.Consumer<String> handler) {
        this.pageRequest = handler;
    }
    // ?

    @FXML
    private void initialize() {

        // Sanity prints help catch fx:id/paths issues quickly
        LOGGER.info("[Explore] init: searchField=" + searchField
                + ", filterBox=" + filterBox + ", resultsPane=" + resultsPane);

        // Populate filters
        filterBox.getItems().setAll("All", "Artists", "Designs", "Completed Tattoos");
        filterBox.getSelectionModel().selectFirst();

        // Mock data for now
        allItems = mockData();

        // Listeners
        searchField.textProperty().addListener((o, a, b) -> refreshResults());
        filterBox.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshResults());

        // Initial fill
        refreshResults();
    }

    private void refreshResults() {
        String q = (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase(Locale.ROOT);
        Kind filter = switch (filterBox.getSelectionModel().getSelectedItem()) {
            case "Artists" -> Kind.ARTISTS;
            case "Designs" -> Kind.DESIGNS;
            case "Completed Tattoos" -> Kind.COMPLETED_TATTOOS;
            default -> Kind.ALL;
        };

        List<SearchItem> filtered = allItems.stream()
                .filter(it -> filter == Kind.ALL || it.kind == filter)
                .filter(it -> q.isEmpty()
                        || it.title.toLowerCase(Locale.ROOT).contains(q)
                        || it.tags.stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(q)))
                .toList();

        resultsPane.getChildren().setAll(
                filtered.isEmpty()
                        ? List.of(new Label("No results. Try a different search or filter."))
                        : filtered.stream().map(this::card).collect(Collectors.toList()));

        LOGGER.info("[Explore] results=" + filtered.size()
                + " filter=" + filterBox.getSelectionModel().getSelectedItem()
                + " q=\"" + q + "\"");
    }

    private Node card(SearchItem item) {
        ImageView iv = new ImageView();
        var res = getClass().getResourceAsStream(item.thumbnail);
        if (res != null) {
            iv.setImage(new Image(res, 220, 0, true, true));
            iv.setFitWidth(220);
            iv.setPreserveRatio(true);
        }

        // create the hover overlay label
        Label overlay = new Label(item.hoverText);
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

        // stack image + overlay so overlay sits on top
        StackPane imageStack = new StackPane(iv, overlay);

        Label title = new Label(item.title);
        Label kind = new Label(switch (item.kind) {
            case ARTISTS -> "Artist";
            case DESIGNS -> "Design";
            case COMPLETED_TATTOOS -> "Completed Tattoo";
            default -> "";
        });
        Label tags = new Label(String.join(" • ", item.tags));
        tags.setWrapText(true);

        VBox box = new VBox(8, imageStack, title, kind, tags);
        box.setPrefWidth(240);
        box.setMinWidth(240);
        box.setMaxWidth(240);
        box.setStyle("-fx-padding:10; -fx-background-radius:12; -fx-background-color: rgba(255,255,255,0.05);");

        // smooth hover animation
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

        // Click behaviors
        box.setOnMouseClicked(e -> {
            switch (item.kind) {
                case DESIGNS -> {
                    var SAVE = new javafx.scene.control.ButtonType("Save Locally");
                    var SEND = new javafx.scene.control.ButtonType("Send to Workspace");
                    var CANCEL = javafx.scene.control.ButtonType.CANCEL;

                    var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    alert.setHeaderText("What do you want to do?");
                    alert.setContentText("Design: " + item.title);
                    alert.getButtonTypes().setAll(SEND, SAVE, CANCEL);

                    var choice = alert.showAndWait().orElse(CANCEL);
                    if (choice == SAVE) {
                        saveImageToLocal(iv.getImage(), item.title);
                    } else if (choice == SEND) {
                        exportToWorkspace(iv.getImage(), item.title);
                    }
                }

                case COMPLETED_TATTOOS -> enlargeImage(iv.getImage(), item.title);
                case ARTISTS -> openArtistPage(item.title);
                default -> LOGGER.info("[Explore] clicked " + item.title);
            }
        });

        return box;
    }

    private List<SearchItem> mockData() {
        List<SearchItem> list = new ArrayList<>();

        // Artist(s)
        list.add(new SearchItem(
                "Raven — abstract blackwork",
                Kind.ARTISTS,
                "/icons/artist_raven.jpg",
                List.of("blackwork", "abstract", "linework"),
                "Raven is a punk-studio artist known for bold blackwork and geometric abstractions."));

        // Completed tattoos
        list.add(new SearchItem(
                "Dragon forearm (completed)",
                Kind.COMPLETED_TATTOOS,
                "/icons/completed_dragon_forearm.jpg",
                List.of("forearm", "dragon", "completed"),
                "Healed forearm piece with a stylized dragon—placed in the 3D workspace."));
        list.add(new SearchItem(
                "Koi sleeve segment (completed)",
                Kind.COMPLETED_TATTOOS,
                "/icons/completed_koi_leg.jpg",
                List.of("leg", "koi", "color", "completed"),
                "Color koi segment wrapped on a darker skin tone for realistic preview."));

        // Design library (pick any / all you like)
        list.add(new SearchItem("Koi design", Kind.DESIGNS, "/icons/design_koi.png",
                List.of("koi", "color", "japanese"), "Vibrant koi with rainbow scales."));
        list.add(new SearchItem("Mandala", Kind.DESIGNS, "/icons/design_mandala.png",
                List.of("mandala", "ornamental"), "Symmetrical mandala—great for sternum or back."));
        list.add(new SearchItem("Floral cluster", Kind.DESIGNS, "/icons/design_flowers.png",
                List.of("flowers", "botanical"), "Bold, high-contrast floral cluster."));
        list.add(new SearchItem("Simple dragon", Kind.DESIGNS, "/icons/design_simple_dragon.png",
                List.of("dragon", "blackwork"), "Minimal blackwork dragon motif."));
        list.add(new SearchItem("Phoenix sketch", Kind.DESIGNS, "/icons/design_phoenix.png",
                List.of("phoenix", "mythical"), "Rising phoenix sketch—dynamic flow."));
        list.add(new SearchItem("Sugar skull + rose", Kind.DESIGNS, "/icons/design_skull_rose.png",
                List.of("skull", "neo traditional"), "Sugar-skull with floral accents."));
        list.add(new SearchItem("Playful snake", Kind.DESIGNS, "/icons/design_snake.png",
                List.of("snake", "fun"), "Cartoon snake—good for forearm or calf."));

        return list;
    }

    private void saveImageToLocal(Image img, String name) {
        try {
            if (img == null)
                return;

            // Create the file in user's Downloads folder
            java.io.File file = new java.io.File(System.getProperty("user.home")
                    + "/Downloads/"
                    + name.replaceAll("\\s+", "_") + ".png");

            javax.imageio.ImageIO.write(
                    javafx.embed.swing.SwingFXUtils.fromFXImage(img, null),
                    "png",
                    file);

            // Show confirmation popup
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setHeaderText("Saved Successfully!");
            alert.setContentText("The design \"" + name + "\" has been saved to:\n" + file.getAbsolutePath());
            alert.showAndWait();

            LOGGER.info("[Explore] Saved to " + file.getAbsolutePath());
        } catch (Exception _) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setHeaderText("Save Failed");
            alert.setContentText("Something went wrong while saving \"" + name + "\".");
            alert.showAndWait();
        }
    }

    private void enlargeImage(Image img, String title) {
        if (img == null)
            return;
        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
        iv.setPreserveRatio(true);
        iv.setFitWidth(600);

        javafx.scene.Scene scene = new javafx.scene.Scene(new javafx.scene.layout.StackPane(iv), 700, 700);
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.setTitle(title);
        popup.setScene(scene);
        popup.show();
    }

    private void openArtistPage(String artistName) {
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/app/view/ArtistProfile.fxml"));
            javafx.scene.Parent root = loader.load(); // ✅ Parent

            ArtistProfileController controller = loader.getController();

            String photo = "/icons/artist_raven.jpg";
            String bio = """
                    Raven (she/her) is a punk-studio tattoo artist specializing in bold blackwork,
                    geometric abstractions, and negative-space composition. She loves sleeves and
                    large-scale projects that flow with the body.
                    """;

            // If you add more artists later, switch on name here.
            controller.setData(artistName, bio, photo);

            var stage = new javafx.stage.Stage();
            stage.setTitle("Artist Profile: " + artistName);
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (Exception _) {
            // failed to open artist page; nothing else we can do here
        }
    }

}
