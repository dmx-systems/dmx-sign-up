package systems.dmx.signup.model;

import java.time.Instant;

public class NewAccountToken {

    public NewAccountData accountData;
    public String password;
    public Instant expiration;

    public NewAccountToken(NewAccountData accountData, String password, Instant expiration) {
        this.accountData = accountData;
        this.password = password;
        this.expiration = expiration;
    }
}
