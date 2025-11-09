package app.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GalleryController implements RootController.PageAware, RootController.WorkspaceAware {

    private static final int GALLERY_COLUMNS = 3;
    private static final List<ResourceTattoo> BUILT_IN_TATTOOS = List.of(
        new ResourceTattoo("Dragon Sigil", "/icons/dragon.png"),
        new ResourceTattoo("Signal Glyph", "/icons/sigal.png"),
        new ResourceTattoo("Code Pattern", "/icons/code.png"),
        new ResourceTattoo("Download Crest", "/icons/download.png"),
        new ResourceTattoo("Explore Stamp", "/icons/explore.png"),
        new ResourceTattoo("Map Crest", "/icons/map.png"),
        new ResourceTattoo("Gallery Badge", "/icons/gallery.png"),
        new ResourceTattoo("Workspace Emblem", "/icons/workspace.png"),
        new ResourceTattoo("Tattoo Portrait", "/icons/tattooGuy.jpg")
    );

    @FXML private GridPane galleryGrid;
    @FXML private Button uploadButton;

    private final List<GalleryItem> galleryItems = new ArrayList<>();
    private Consumer<String> onPageRequest;
    private Supplier<WorkspaceController> workspaceProvider;

    @FXML
    private void initialize() {
        loadBuiltInItems();
        renderGallery();
    }

    @FXML
    private void handleUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Add Tattoo To Gallery");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"),
            new FileChooser.ExtensionFilter("PNG", "*.png"),
            new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg")
        );
        Window window = resolveWindow();
        File file = chooser.showOpenDialog(window);
        if (file == null) {
            return;
        }
        Image image = new Image(file.toURI().toString());
        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            showAlert(Alert.AlertType.ERROR, "Unable to load image", "The selected file is not a supported image type.");
            return;
        }
        galleryItems.add(0, new GalleryItem(file.getName(), image));
        renderGallery();
    }

    @Override
    public void setOnPageRequest(Consumer<String> handler) {
        this.onPageRequest = handler;
    }

    @Override
    public void setWorkspaceProvider(Supplier<WorkspaceController> provider) {
        this.workspaceProvider = provider;
    }

    private void loadBuiltInItems() {
        if (!galleryItems.isEmpty()) {
            return;
        }
        for (ResourceTattoo asset : BUILT_IN_TATTOOS) {
            Image image = loadResourceImage(asset.path());
            if (image != null) {
                galleryItems.add(new GalleryItem(asset.title(), image));
            }
        }
    }

    private Image loadResourceImage(String path) {
        try (InputStream stream = GalleryController.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            Image image = new Image(stream);
            return image.isError() ? null : image;
        } catch (IOException ex) {
            return null;
        }
    }

    private void renderGallery() {
        if (galleryGrid == null) {
            return;
        }
        galleryGrid.getChildren().clear();
        int column = 0;
        int row = 0;
        for (GalleryItem item : galleryItems) {
            Node card = createCard(item);
            galleryGrid.add(card, column, row);
            column++;
            if (column >= GALLERY_COLUMNS) {
                column = 0;
                row++;
            }
        }
    }

    private Node createCard(GalleryItem item) {
        ImageView imageView = new ImageView(item.image());
        imageView.setFitWidth(180);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("gallery-image");

        StackPane preview = new StackPane(imageView);
        preview.getStyleClass().add("gallery-preview");
        preview.setPrefSize(180, 180);

        Label caption = new Label(item.title());
        caption.getStyleClass().add("gallery-caption");
        caption.setWrapText(true);
        caption.setMaxWidth(180);

        VBox card = new VBox(6, preview, caption);
        card.getStyleClass().add("gallery-card");
        card.setPrefWidth(180);

        ContextMenu menu = createContextMenu(item);
        installInteractions(card, item, menu);
        installInteractions(preview, item, menu);
        installInteractions(imageView, item, menu);

        return card;
    }

    private ContextMenu createContextMenu(GalleryItem item) {
        MenuItem openItem = new MenuItem("Open in workspace");
        openItem.setOnAction(e -> openInWorkspace(item));
        return new ContextMenu(openItem);
    }

    private void installInteractions(Node node, GalleryItem item, ContextMenu menu) {
        node.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                openInWorkspace(item);
            } else if (event.getButton() == MouseButton.SECONDARY) {
                menu.show(node, event.getScreenX(), event.getScreenY());
            }
        });
        node.setOnContextMenuRequested(event -> menu.show(node, event.getScreenX(), event.getScreenY()));
    }

    //IMPORTANT: This method depends on WorkspaceController's openTattooFromGallery method.
    private void openInWorkspace(GalleryItem item) {
        WorkspaceController workspace = workspaceProvider != null ? workspaceProvider.get() : null;
        if (workspace == null) {
            showAlert(Alert.AlertType.INFORMATION, "Workspace unavailable", "Open the workspace at least once before sending tattoos from the gallery.");
            return;
        }
        boolean accepted = workspace.openTattooFromGallery(item.image(), item.title());
        if (!accepted) {
            showAlert(Alert.AlertType.INFORMATION, "Tattoo placement not ready", "Load a model with UVs in the workspace before adding tattoos.");
            return;
        }
        if (onPageRequest != null) {
            onPageRequest.accept("workspace");
        }
    }

    private Window resolveWindow() {
        if (uploadButton != null && uploadButton.getScene() != null) {
            return uploadButton.getScene().getWindow();
        }
        if (galleryGrid != null && galleryGrid.getScene() != null) {
            return galleryGrid.getScene().getWindow();
        }
        return null;
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
    }

    private record GalleryItem(String title, Image image) {
        GalleryItem {
            Objects.requireNonNull(image, "image");
        }
    }

    private record ResourceTattoo(String title, String path) {}
}
