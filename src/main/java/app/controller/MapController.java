package app.controller;

import java.util.function.Consumer;
import javafx.concurrent.Task;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.entity.Profile;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;

import app.controller.RootController.PageAware;
import app.controller.RootController.ProfileAware;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import com.gluonhq.maps.MapLayer;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Pair;

import java.util.LinkedList;

import app.entity.ProfileCell;
import app.entity.DatabaseConnector;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

public class MapController implements PageAware, ProfileAware {
    private static final Logger LOGGER = Logger.getLogger(MapController.class.getName());

    @FXML
    private TextField searchField;

    @FXML
    private ListView<Profile> resultsList;

    private LinkedList<Profile> allResults;

    @FXML
    private StackPane mapContainer;

    private MapView map;

    private final List<MapLayer> layers = new LinkedList<>();

    private Consumer<String> onPageRequest;
    private Consumer<Profile> profileRequest;

    public void setOnPageRequest(Consumer<String> handler) {
        this.onPageRequest = handler;
    }

    @Override
    public void setProfileProvider(Consumer<Profile> provider) {
        this.profileRequest = provider;
    }

    @FXML
    public void initialize() {
        map = new MapView();
        MapPoint center = new MapPoint(41.892718, 12.476259);
        map.setCenter(center);
        map.setZoom(10);
        mapContainer.getChildren().add(map);

        resultsList.setCellFactory(listView -> new ProfileCell(this::handleProfileSelection));
    }

    // Inner layer class
    static class DotLayer extends MapLayer {
        private final List<MapPoint> points;

        DotLayer(List<MapPoint> points) {
            this.points = points;
        }

        @Override
        protected void layoutLayer() {
            getChildren().clear();
            for (MapPoint p : points) {
                Point2D mp = getMapPoint(p.getLatitude(), p.getLongitude());
                Node dot = new Circle(4, Color.RED);
                dot.setTranslateX(mp.getX());
                dot.setTranslateY(mp.getY());
                getChildren().add(dot);
            }
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        resultsList.getItems().clear();
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        if (query.charAt(0) == '#') {
            tagSearch(query);
        }
        if (query.charAt(0) == '@') {
            usernameSearch(query.substring(1).trim());
        }
        populateMapAsync();
        boolean foundCity = citySearch(query);
        if (!foundCity) {
            usernameSearch(query);
            populateMapAsync();
        } else {
            populateMapAsync();
        }
        if (allResults == null || allResults.isEmpty())
            return;
        resultsList.getItems().setAll(allResults);
    }

    private void populateMapAsync() {
        if (allResults == null || allResults.isEmpty())
            return;

        depopulateMap(); // clear old markers first

        Task<DotLayer> task = buildDotLayerTask();

        task.setOnSucceeded(e -> {
            depopulateMap();
            DotLayer layer = task.getValue();
            map.addLayer(layer);
            layers.add(layer);
            if (!allResults.isEmpty()) {
                adjustViewport();
            }

            map.requestLayout(); // ensure refresh
            double z = map.getZoom();
            map.setZoom(z + 0.0001);
            map.setZoom(z);
        });

        task.setOnFailed(e -> logTaskError(task));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private Task<DotLayer> buildDotLayerTask() {
        return new Task<>() {
            @Override
            protected DotLayer call() {
                List<MapPoint> points = new java.util.ArrayList<>();
                int count = allResults.size();
                for (int i = 0; i < count; i++) {
                    Profile target = allResults.get(i);
                    points.add(new MapPoint(target.getWorkLatitude(), target.getWorkLongitude()));
                }
                return new DotLayer(points);
            }
        };
    }

    private void adjustViewport() {
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (Profile p : allResults) {
            double lat = p.getWorkLatitude();
            double lon = p.getWorkLongitude();
            minLat = Math.min(minLat, lat);
            maxLat = Math.max(maxLat, lat);
            minLon = Math.min(minLon, lon);
            maxLon = Math.max(maxLon, lon);
        }

        double centerLat = (minLat + maxLat) / 2.0;
        double centerLon = (minLon + maxLon) / 2.0;
        double spread = Math.max(maxLat - minLat, maxLon - minLon);

        map.setCenter(new MapPoint(centerLat, centerLon));
        map.setZoom(calculateZoom(spread));
    }

    private int calculateZoom(double spread) {
        if (spread < 0.01)
            return 14;
        if (spread < 0.05)
            return 12;
        if (spread < 0.2)
            return 10;
        if (spread < 1.0)
            return 8;
        return 6;
    }

    private void logTaskError(Task<DotLayer> task) {
        Throwable ex = task.getException();
        if (ex != null) {
            LOGGER.log(Level.SEVERE, "Map task failed", ex);
        }
    }

    private void depopulateMap() {
        for (MapLayer layer : layers) {
            map.removeLayer(layer);
        }
        layers.clear();
    }

    private void tagSearch(String query) {
        // TODO: implement tag search
    }

    private boolean citySearch(String query) {
        try {// Geocode city name

            Pair<Double, Double> center = geocodeCity(query.trim());
            if (center == null) {
                return false;
            }

            double lat = center.getKey();
            double lon = center.getValue();

            // Define a search radius maybe slider later
            double radius = 0.5;
            double latFrom = lat - radius;
            double latTo = lat + radius;
            double lonFrom = lon - radius;
            double lonTo = lon + radius;

            // Query database for users within bounds
            List<Profile> profiles = DatabaseConnector.getProfilesWithinBounds(latFrom, latTo, lonFrom, lonTo);
            // ui update
            if (profiles != null && !profiles.isEmpty()) {
                // add style fitler logic here
                allResults = new LinkedList<>(profiles);
            } else {
                return false;
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching by city: " + query, e);
        }
        return false;
    }

    private Pair<Double, Double> geocodeCity(String city) {
        try {
            String urlStr = "https://nominatim.openstreetmap.org/search?format=json&q="
                    + java.net.URLEncoder.encode(city.toLowerCase(), "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "JavaFXApp");
            conn.setRequestMethod("GET");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                JSONArray arr = new JSONArray(sb.toString());
                if (arr.length() > 0) {
                    JSONObject obj = arr.getJSONObject(0);
                    double lat = obj.getDouble("lat");
                    double lon = obj.getDouble("lon");
                    return new Pair<>(lat, lon);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to geocode city: " + city, e);
        }
        return null;
    }

    private void usernameSearch(String query) {
        try {
            // Fetch all profiles whose usernames partially match the query
            Profile profile = DatabaseConnector.getProfileByUsername(query.trim());

            if (profile != null) {
                allResults = new LinkedList<>();
                allResults.add(profile);
            } else {
                LOGGER.info("No matching profiles found.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving profiles for query: " + query, e);
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        depopulateMap();
        if (searchField != null) {
            searchField.clear();
        }
        if (resultsList != null) {
            resultsList.getItems().clear();
        }
    }

    @FXML
    private void filter(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String filterTag = clickedButton.getText(); // e.g. "#Blackwork"
        applyFilter(filterTag);
        tagSearch(filterTag);
    }

    private void applyFilter(String tag) {
        resultsList.getItems().setAll(
                allResults.stream()
                        .filter(item -> item.getStylesList().contains(tag.substring(1)))
                        .toList());
    }

    private void handleProfileSelection(Profile profile) {
        if (profile == null) {
            return;
        }
        if (profileRequest != null) {
            profileRequest.accept(profile);
        }
    }
}
