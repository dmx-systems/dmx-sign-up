package systems.dmx.signup.model;

import java.time.Instant;

public class PasswordResetToken {

    public NewAccountData accountData;
    public Instant expiration;
    public String redirectUrl;

    public PasswordResetToken(NewAccountData accountData, Instant expiration, String redirectUrl) {
        this.accountData = accountData;
        this.expiration = expiration;
        this.redirectUrl = redirectUrl;
    }
}
