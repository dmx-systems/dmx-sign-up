package systems.dmx.signup;

public class SignUpRequestResult {

    public final Code code;

    public final String username;

    public SignUpRequestResult(Code code) {
        this(code, null);
    }

    public SignUpRequestResult(Code code, String username) {
        this.code = code;
        this.username = username;
    }

    public enum Code {
        ACCOUNT_CREATION_DENIED,
        ADMIN_PRIVILEGE_MISSING,
        SUCCESS_EMAIL_CONFIRMATION_NEEDED,
        SUCCESS_ACCOUNT_PENDING,
        SUCCESS_ACCOUNT_CREATED,

        ERROR_INVALID_EMAIL,
        UNEXPECTED_ERROR
    }
}