package systems.dmx.signup;

public class ProcessSignUpRequestResult {

    public final Code code;
    public final String username;

    public ProcessSignUpRequestResult(Code code) {
        this(code, null);
    }

    public ProcessSignUpRequestResult(Code code, String username) {
        this.code = code;
        this.username = username;
    }

    public enum Code {
        SUCCESS, INVALID_TOKEN, LINK_EXPIRED, SUCCESS_ACCOUNT_PENDING, UNEXPECTED_ERROR
    }
}