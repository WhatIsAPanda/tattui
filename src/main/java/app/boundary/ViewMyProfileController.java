package app.boundary;

import app.entity.LoggedInUser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ViewMyProfileController {
    @FXML
    private ScrollPane mainScreen;
    @FXML
    private AnchorPane loadingScreen;
    @FXML
    private Text artistNameField1;
    @FXML
    private Text artistNameField2;
    @FXML
    private Circle profilePicture;
    @FXML
    private Text biographyField;
    @FXML
    private GridPane postsPanel;

    public class GetImageFromURLTask implements Callable<Image> {
        private String url;
        public GetImageFromURLTask(String url) {
            this.url = url;
        }
        public Image call() throws Exception {
            return new Image(this.url);
        }
    }
    @FXML
    public void initialize() throws InterruptedException, ExecutionException, SQLException {
        loadingScreen.setVisible(true);
        mainScreen.setVisible(false);
        String url = "jdbc:mysql://hopper.proxy.rlwy.net:28919/railway";
        String user = "root";
        String password = "lLcIaxfWngHgBcPljtAUtuoebZSDSPtz";
        String userName = LoggedInUser.getUsername();
        Thread loadProfileInfo = new Thread(() -> {
            try {

                artistNameField1.setText("@" + userName);
                artistNameField2.setText("@" + userName);
                Connection con = DriverManager.getConnection(url,user, password);
                PreparedStatement stmt = con.prepareStatement(
                        "SELECT profile_picture, biography, profile_picture\n" +
                        "FROM Users \n" +
                        "WHERE username = ?" +
                                "LIMIT 1;");
                stmt.setString(1,userName);
                ResultSet profileInfo = stmt.executeQuery();
                profileInfo.next();

                String imageURL = profileInfo.getString("profile_picture");
                String biography = profileInfo.getString("biography");
                biographyField.setText(biography);

                Image image = new Image(imageURL);
                ImagePattern pfpImagePattern = new ImagePattern(image);
                profilePicture.setFill(pfpImagePattern);
                con.close();

            } catch (Exception e) {
                System.out.println(e);
                throw new RuntimeException(e);
            }
            loadingScreen.setVisible(false);
            mainScreen.setVisible(true);
        });
        loadProfileInfo.start();
        Thread loadArtistInfo = new Thread(() -> {
            try {
                Connection con = DriverManager.getConnection(url,user, password);
                PreparedStatement stmt = con.prepareStatement("SELECT \n" +
                        "P.postURL,\n" +
                        "P.caption\n" +
                        "FROM PostOwnerships as PO\n" +
                        "LEFT JOIN Users as U ON PO.user_id =  U.id\n" +
                        "LEFT JOIN Posts as P on PO.post_id = P.id\n" +
                        "WHERE username = ?;");
                stmt.setString(1,userName);
                ResultSet rs = stmt.executeQuery();

                ExecutorService executor = Executors.newFixedThreadPool(5);
                List<Callable<Image>> taskList = new ArrayList<>();
                while (rs.next()) {
                    taskList.add(new GetImageFromURLTask(rs.getString("postURL")));
                }
                List<Future<Image>> imageResults = executor.invokeAll(taskList);
                List<Image> imageList = new ArrayList<>();
                for (Future<Image> imageResult : imageResults) {
                    imageList.add(imageResult.get());
                }
                Platform.runLater(() -> {
                    int imagesProcessed = 0;
                    int row = -1;
                    while(imagesProcessed < imageList.size()) {
                        row++;
                        RowConstraints constraints = new RowConstraints();
                        constraints.setMaxHeight(250);
                        constraints.setPrefHeight(250);
                        postsPanel.getRowConstraints().add(constraints);
                        for(int col = 0; col < postsPanel.getColumnCount(); col++) {
                            if(imagesProcessed == imageList.size()) {
                                break;
                            }
                            ImageView imgView = new ImageView(imageList.get(imagesProcessed));
                            imgView.setFitHeight(250);
                            imgView.setFitWidth(179);
                            imgView.setPreserveRatio(false);

                            postsPanel.add(imgView,col,row);
                            imagesProcessed++;
                        }

                    }
                });

                con.close();
            }
            catch (Exception e) {
                System.out.println(e);
            }
        });
        loadArtistInfo.start();


    }

}
