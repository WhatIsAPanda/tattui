package app.entity;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import java.io.IOException;
import app.entity.Profile;

public class ProfileCell extends ListCell<Profile> {

    @Override
    protected void updateItem(Profile profile, boolean empty) {
        super.updateItem(profile, empty);

        if (empty || profile == null) {
            setGraphic(null);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/profileMapCard.fxml"));
            HBox root = loader.load();

            //getting the boxes from the fmxl
            //profile pic
            ImageView image = (ImageView) root.getChildren().get(0);
            VBox info = (VBox) root.getChildren().get(1);

            //UserName?
            Label title = (Label) info.getChildren().get(0);
            //Bio
            Label desc = (Label) info.getChildren().get(1);
            //Adderess
            Label address = (Label) info.getChildren().get(2);
            //Tag
            Label tag = (Label) info.getChildren().get(3);

            //Setting them
            title.setText(profile.getUsername());
            desc.setText(profile.getBiography());
            address.setText(profile.getAddress());
            tag.setText(profile.getTags());

            if (profile.getProfile_picture_url() != null && !profile.getProfile_picture_url().isEmpty()) {
                image.setImage(new Image(profile.getProfile_picture_url(), true));
            }

            setGraphic(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
