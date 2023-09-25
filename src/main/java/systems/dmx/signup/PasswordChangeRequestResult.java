package systems.dmx.signup;

import systems.dmx.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;

public enum PasswordChangeRequestResult implements JSONEnabled {

    SUCCESS,
    PASSWORD_CHANGE_FAILED,
    NO_TOKEN,
    UNEXPECTED_ERROR;

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject().put("result", name());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}
