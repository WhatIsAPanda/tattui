package app.controller;
import java.sql.*;

import app.entity.*;


public class LoginController {

    public static boolean verifyUser(String username, String password) throws SQLException {
        Account account = DatabaseConnector.getAccountByUsername(username);
        if(account == null) {
            return false;
        }
        if(account.getPassword().equals(password)) {
            LoggedInAccount.setInstance(account);
            Profile artistProfile = DatabaseConnector.getProfileByUsername(username);
            LoggedInProfile.setInstance(artistProfile);
            return true;
        }
        return false;
    }


}
