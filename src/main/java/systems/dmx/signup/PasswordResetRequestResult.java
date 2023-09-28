package systems.dmx.signup;

import systems.dmx.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;

public class PasswordResetRequestResult implements JSONEnabled {

    public final Code code;

    public final String username;
    public final String email;
    public final String displayName;
    public final String redirectUrl;

    public PasswordResetRequestResult(Code code) {
        this(code, null, null, null, null);
    }

    public PasswordResetRequestResult(Code code, String username, String email, String displayName,
                                      String redirectUrl) {
        this.code = code;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.redirectUrl = redirectUrl;
    }

    public enum Code {
        SUCCESS,
        INVALID_TOKEN,
        LINK_EXPIRED,
        UNEXPECTED_ERROR
    }

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject().put("result", code.name());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}
