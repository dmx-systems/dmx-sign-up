package systems.dmx.signup.model;

public class NewAccountData {
    public String methodName;
    public String username;
    public String emailAddress;
    public String displayName;

    public NewAccountData(String methodName, String username, String emailAddress, String displayName) {
        this.methodName = methodName;
        this.username = username;
        this.emailAddress = emailAddress;
        this.displayName = displayName;
    }
}
