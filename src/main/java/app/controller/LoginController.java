package app.controller;
import java.sql.*;
import app.entity.DatabaseConnector;
import app.entity.LoggedInUser;
import app.entity.Profile;


public class LoginController {

    public static boolean verifyUser(String username, String password) throws SQLException {
        Profile profile = DatabaseConnector.getProfileByUsername(username);
        if(profile == null) {
            return false;
        }
        if(profile.getPassword().equals(password)) {
            LoggedInUser.setInstance(profile);
            return true;
        }
        return false;
    }


}
