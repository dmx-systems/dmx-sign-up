package systems.dmx.signup.model;

public class NewAccountData {
    public String username;
    public String emailAddress;
    public String displayName;

    public NewAccountData(String username, String emailAddress, String displayName) {
        this.username = username;
        this.emailAddress = emailAddress;
        this.displayName = displayName;
    }
}
