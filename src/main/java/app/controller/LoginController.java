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
        if(profile.password.equals(password)) {
            LoggedInUser.setInstance(profile);
            System.out.println(LoggedInUser.getInstance().username);
            return true;
        }
        return false;
    }


}
