package app.entity;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class LoggedInProfile {
    private static final ObjectProperty<Profile> PROFILE = new SimpleObjectProperty<>();

    private LoggedInProfile() {
    }

    public static Profile getInstance() {
        return PROFILE.get();
    }

    public static void setInstance(Profile profile) {
        PROFILE.set(profile);
    }

    public static ReadOnlyObjectProperty<Profile> profileProperty() {
        return PROFILE;
    }
}
