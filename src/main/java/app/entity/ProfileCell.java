package app.entity;

import app.util.ImageResolver;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class ProfileCell extends ListCell<Profile> {

    private final Consumer<Profile> onClick;

    public ProfileCell() {
        this(null);
    }

    public ProfileCell(Consumer<Profile> onClick) {
        this.onClick = onClick;
    }

    @Override
    protected void updateItem(Profile profile, boolean empty) {
        super.updateItem(profile, empty);

        if (empty || profile == null) {
            setGraphic(null);
            setText(null);
            setOnMouseClicked(null);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/profileCellCard.fxml"));
            HBox root = loader.load();

            ImageView image = (ImageView) root.getChildren().get(0);
            VBox info = (VBox) root.getChildren().get(1);

            Label title = (Label) info.getChildren().get(0);
            Label desc = (Label) info.getChildren().get(1);
            Label address = (Label) info.getChildren().get(2);
            Label tag = (Label) info.getChildren().get(3);

            title.setText(profile.getUsername());
            desc.setText(profile.getBiography());
            address.setText(profile.getAddress());
            List<String> stylesList = profile.getStylesList();
            StringBuilder styles = new StringBuilder();
            for (String style : stylesList) {
                styles.append("#").append(style).append(" ");
            }
            tag.setText(styles.toString());

            String profilePictureUrl = profile.getProfilePictureURL();
            if (profilePictureUrl != null && !profilePictureUrl.isBlank()) {
                try {
                    Image img = ImageResolver.load(profilePictureUrl, 0, 0, true, true, true);
                    image.setImage(img);

                    img.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (img.getProgress() == 1.0 && img.getWidth() > 0 && img.getHeight() > 0) {
                            double width = img.getWidth();
                            double height = img.getHeight();
                            double size = Math.min(width, height);
                            double x = (width - size) / 2;
                            double y = (height - size) / 2;

                            image.setViewport(new Rectangle2D(x, y, size, size));
                            image.setFitWidth(78);
                            image.setFitHeight(78);
                            image.setPreserveRatio(false);
                        }
                    });
                } catch (IllegalArgumentException _) {
                    image.setImage(null);
                }
            } else {
                image.setImage(null);
            }

            setGraphic(root);
            setPadding(Insets.EMPTY);
            if (onClick != null) {
                setOnMouseClicked(evt -> onClick.accept(profile));
            } else {
                setOnMouseClicked(null);
            }

        } catch (IOException _) {
            setGraphic(null);
        }
    }
}
