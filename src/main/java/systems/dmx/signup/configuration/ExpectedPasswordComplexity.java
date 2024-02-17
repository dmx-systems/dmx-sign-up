package systems.dmx.signup.configuration;

public enum ExpectedPasswordComplexity {
    NONE, COMPLEX;

    public static ExpectedPasswordComplexity fromStringOrComplex(String value) {
        try {
            return ExpectedPasswordComplexity.valueOf(value.trim().toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            return COMPLEX;
        }
    }
}
