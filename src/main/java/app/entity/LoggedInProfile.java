package app.entity;

public class LoggedInProfile {
    private static Profile loggedInProfile = null;

    private LoggedInProfile(Profile profile) {
        LoggedInProfile.loggedInProfile = profile;
    }

    public static Profile getInstance() {
        return loggedInProfile;
    }

    public static void setInstance(Profile profile) {
        LoggedInProfile.loggedInProfile = profile;
    }
}
