package systems.dmx.signup.configuration;

public enum AccountCreation {
    DISABLED, ADMIN, PUBLIC;

    static AccountCreation fromStringOrDisabled(String value) {
        try {
            return AccountCreation.valueOf(value.trim().toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            return DISABLED;
        }
    }
}
