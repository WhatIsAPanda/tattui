package app.entity;

public class LoggedInAccount {
    private static Account loggedInAccount = null;

    private LoggedInAccount(Account account) {
        LoggedInAccount.loggedInAccount = account;
    }

    public static Account getInstance() {
        return loggedInAccount;
    }

    public static void setInstance(Account account) {
        LoggedInAccount.loggedInAccount = account;
    }
}
