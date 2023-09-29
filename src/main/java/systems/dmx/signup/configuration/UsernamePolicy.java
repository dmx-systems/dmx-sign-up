package systems.dmx.signup.configuration;

public enum UsernamePolicy {
    UNCONFINED, DISPLAYNAME_IS_USERNAME_, USERNAME_IS_EMAIL;

    static UsernamePolicy fromStringOrAgnostic(String value) {
        try {
            return UsernamePolicy.valueOf(value.trim().toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            return UNCONFINED;
        }
    }
}
