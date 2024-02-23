package systems.dmx.signup.usecase;

import org.passay.*;
import systems.dmx.signup.configuration.ExpectedPasswordComplexity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IsPasswordComplexEnoughUseCase {

    public static final int MINIMUM_PASSWORD_LENGTH = 8;

    public static final int MAXIMUM_PASSWORD_LENGTH = 16;

    private final PasswordValidator validator = new PasswordValidator(
            new LengthRule(MINIMUM_PASSWORD_LENGTH, MAXIMUM_PASSWORD_LENGTH),
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            new CharacterRule(EnglishCharacterData.LowerCase, 1),
            new CharacterRule(EnglishCharacterData.Digit, 1),
            new CharacterRule(EnglishCharacterData.Special, 1),
            new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 5, false),
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 5, false),
            new IllegalSequenceRule(EnglishSequenceData.USQwerty, 5, false),
            new WhitespaceRule()
    );

    @Inject
    IsPasswordComplexEnoughUseCase() {}

    public boolean invoke(ExpectedPasswordComplexity expectedPasswordComplexity, String password) {
        switch (expectedPasswordComplexity) {
            case NONE:
                return true;
            case COMPLEX:
                return validator.validate(new PasswordData(password)).isValid();
            default:
                throw new IllegalStateException("Unexpected password complexity: " + expectedPasswordComplexity);
        }
    }

}
