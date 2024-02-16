package systems.dmx.signup.configuration;

public enum AccountCreation {
    DISABLED, ADMIN, PUBLIC;

    public static AccountCreation fromStringOrDisabled(String value) {
        try {
            return AccountCreation.valueOf(value.trim().toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            return DISABLED;
        }
    }

    public enum PasswordHandling {
        EDITABLE, GENERATED;

        public static PasswordHandling fromStringOrEditable(String value) {
            try {
                return PasswordHandling.valueOf(value.trim().toUpperCase());
            } catch (NullPointerException | IllegalArgumentException e) {
                return EDITABLE;
            }
        }
    }
}
