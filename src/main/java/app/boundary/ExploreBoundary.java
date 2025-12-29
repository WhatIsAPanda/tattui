package app.boundary;

import app.entity.DatabaseConnector;
import app.entity.Post;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ExploreBoundary {
    @FXML
    private GridPane explorePane;

    @FXML
    public void initialize() throws SQLException, IOException {
        List<Post> posts =  DatabaseConnector.getLatestPosts();
        System.out.println(posts.size());
        int imagesProcessed = 0;
        int row = -1;
        while(imagesProcessed < posts.size()) {
            row++;
            RowConstraints constraints = new RowConstraints();
            constraints.setMaxHeight(360);
            constraints.setMinHeight(360);
            explorePane.getRowConstraints().add(constraints);
            for(int col = 0; col < explorePane.getColumnCount(); col++) {
                if(imagesProcessed == posts.size()) {
                    break;
                }
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/PostItem.fxml"));
                ImageView imgView = loader.load();

                PostItemBoundary postItemController = loader.getController();
                postItemController.setPost(posts.get(imagesProcessed));
                imgView.setFitHeight(360);
                imgView.setFitWidth(360);
                imgView.setPreserveRatio(false);

                explorePane.add(imgView, col, row);
                imagesProcessed++;
            }
        }

    }
}
