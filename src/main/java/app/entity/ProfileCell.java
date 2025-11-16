package app.entity;

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

public class ProfileCell extends ListCell<Profile> {

    @Override
    protected void updateItem(Profile profile, boolean empty) {
        super.updateItem(profile, empty);

        if (empty || profile == null) {
            setGraphic(null);
            setText(null);
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
            List<String> styles_list = profile.getStylesList();
            StringBuilder styles = new StringBuilder();
            for(String style : styles_list) {
                styles.append(style);
            }
            tag.setText(styles.toString());

            if (profile.getProfilePicture() != null) {
                Image img = profile.getProfilePicture();
                image.setImage(img);

                // Wait until image loads to set the viewport (image must have real dimensions)
                img.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (img.getProgress() == 1.0 && img.getWidth() > 0 && img.getHeight() > 0) {
                        double width = img.getWidth();
                        double height = img.getHeight();
                        double size = Math.min(width, height);
                        double x = (width - size) / 2;
                        double y = (height - size) / 2;

                        image.setViewport(new Rectangle2D(x, y, size, size));
                        image.setFitWidth(88);
                        image.setFitHeight(88);
                        image.setPreserveRatio(false);
                    }
                });
            } else {
                image.setImage(null);
            }

            setGraphic(root);
            setPadding(Insets.EMPTY);

        } catch (IOException e) {
            e.printStackTrace();
            setGraphic(null);
        }
    }
}
