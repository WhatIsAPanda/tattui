package app.boundary;

import app.entity.Post;
import app.entity.Profile;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.util.List;

public abstract class BaseProfileBoundary {

    protected void populateProfileCommon(Profile profile, Circle profilePicture, Object biographyField,
            Object artistNameField, Object longitudeField, Object latitudeField, Object styles) {
        if (profile == null)
            return;

        if (artistNameField instanceof Text t)
            t.setText(profile.getUsername());
        else if (artistNameField instanceof Label l)
            l.setText(profile.getUsername());
        else if (artistNameField instanceof javafx.scene.control.TextField tf)
            tf.setText(profile.getUsername());

        Image image = profile.getProfilePicture();
        if (image != null && profilePicture != null)
            profilePicture.setFill(new ImagePattern(image));

        String bio = profile.getBiography();
        if (biographyField instanceof Text t)
            t.setText(bio);
        else if (biographyField instanceof TextArea ta)
            ta.setText(bio);
        else if (biographyField instanceof Label l)
            l.setText(bio);

        double longitude = profile.getWorkLongitude();
        double latitude = profile.getWorkLatitude();
        if (longitudeField instanceof javafx.scene.control.TextField lonField)
            lonField.setText(Double.toString(longitude));
        else if (longitudeField instanceof Label lonLabel)
            lonLabel.setText(Double.toString(longitude));
        if (latitudeField instanceof javafx.scene.control.TextField latField)
            latField.setText(Double.toString(latitude));
        else if (latitudeField instanceof Label latLabel)
            latLabel.setText(Double.toString(latitude));

        List<String> stylesList = profile.getStylesList();
        StringBuilder stylesbuilder = new StringBuilder();
        for (String style : stylesList) {
            stylesbuilder.append("#").append(style).append(" ");
        }
        if (styles instanceof Label stylesLabel)
            stylesLabel.setText(stylesbuilder.toString().trim());

    }

    protected void populatePosts(GridPane postsPanel, List<Post> posts) {
        if (postsPanel == null || posts == null || posts.isEmpty())
            return;

        postsPanel.getChildren().clear();
        postsPanel.getRowConstraints().clear();

        int cols = Math.max(postsPanel.getColumnCount(), 3); // Default to 3
        int row = 0;
        int processed = 0;

        while (processed < posts.size()) {
            RowConstraints rowC = new RowConstraints(250);
            postsPanel.getRowConstraints().add(rowC);

            for (int col = 0; col < cols && processed < posts.size(); col++, processed++) {
                Image img = posts.get(processed).getImage();
                if (img != null) {
                    ImageView imgView = new ImageView(img);
                    imgView.setFitWidth(179);
                    imgView.setFitHeight(250);
                    imgView.setPreserveRatio(false);
                    postsPanel.add(imgView, col, row);
                }
            }
            row++;
        }
    }
}
