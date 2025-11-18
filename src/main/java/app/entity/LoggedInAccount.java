package app.entity;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class LoggedInAccount {
    private static final ObjectProperty<Account> ACCOUNT = new SimpleObjectProperty<>();

    private LoggedInAccount() {
    }

    public static Account getInstance() {
        return ACCOUNT.get();
    }

    public static void setInstance(Account account) {
        ACCOUNT.set(account);
    }

    public static ReadOnlyObjectProperty<Account> accountProperty() {
        return ACCOUNT;
    }
}
