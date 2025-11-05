package app.entity;

public class LoggedInUser {
    private static String username;

    public static String getUsername() {
        if(username == null) {
            return null;
        }
        else {
            return username;
        }
    }
    public static void setUsername(String username) {
        LoggedInUser.username = username;
    }
}
