package app.entity;

public class LoggedInUser {
    private static Profile loggedInProfile = null;

    private LoggedInUser(Profile profile) {
        LoggedInUser.loggedInProfile = profile;
    }

    public static Profile getInstance() {
        return loggedInProfile;
    }

    public static void setInstance(Profile profile) {
        LoggedInUser.loggedInProfile = profile;
    }
}
