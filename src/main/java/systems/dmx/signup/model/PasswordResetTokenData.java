package systems.dmx.signup.model;

import java.time.Instant;

public class PasswordResetTokenData {

    public NewAccountData accountData;
    public Instant expiration;
    public String redirectUrl;

    public PasswordResetTokenData(NewAccountData accountData, Instant expiration, String redirectUrl) {
        this.accountData = accountData;
        this.expiration = expiration;
        this.redirectUrl = redirectUrl;
    }
}
