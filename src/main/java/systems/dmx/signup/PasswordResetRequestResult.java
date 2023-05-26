package systems.dmx.signup;

public class PasswordResetRequestResult {

    public final Code code;

    public final String token;
    public final String username;
    public final String email;
    public final String displayName;
    public final String redirectUrl;

    public PasswordResetRequestResult(Code code) {
        this(code, null, null, null, null, null);
    }

    public PasswordResetRequestResult(Code code, String token) {
        this(code, token, null, null, null, null);
    }

    public PasswordResetRequestResult(Code code, String token, String username, String email, String displayName, String redirectUrl) {
        this.token = token;
        this.code = code;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.redirectUrl = redirectUrl;
    }

    public enum Code {
        SUCCESS,
        INVALID_TOKEN,

        LINK_EXPIRED, UNEXPECTED_ERROR

    }
}