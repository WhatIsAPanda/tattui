package app.controller;

import app.entity.DatabaseConnector;
import app.entity.Profile;

import java.sql.SQLException;
import java.util.List;

public class SearchController {
    public static List<Profile> getRelevantProfiles(String userInput) throws SQLException {
        return DatabaseConnector.getProfilesLike(userInput);
    }
}
