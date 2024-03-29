package systems.dmx.signup.usecase;

import org.passay.*;
import systems.dmx.signup.configuration.ExpectedPasswordComplexity;
import systems.dmx.signup.configuration.SignUpConfigOptions;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IsPasswordComplexEnoughUseCase {

    private final PasswordValidator complexValidator;

    private final PasswordValidator simpleValidator;

    IsPasswordComplexEnoughUseCase(int minPasswordLength, int maxPasswordLength) {
        complexValidator = new PasswordValidator(
                new LengthRule(minPasswordLength, maxPasswordLength),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1),
                new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 5, false),
                new IllegalSequenceRule(EnglishSequenceData.Numerical, 5, false),
                new IllegalSequenceRule(EnglishSequenceData.USQwerty, 5, false),
                new WhitespaceRule()
        );

        simpleValidator = new PasswordValidator(
                new LengthRule(minPasswordLength, maxPasswordLength),
                new WhitespaceRule()
        );
    }

    @Inject
    IsPasswordComplexEnoughUseCase() {
        this(SignUpConfigOptions.CONFIG_EXPECTED_MIN_PASSWORD_LENGTH,
                SignUpConfigOptions.CONFIG_EXPECTED_MAX_PASSWORD_LENGTH);
    }

    public boolean invoke(ExpectedPasswordComplexity expectedPasswordComplexity, String password) {
        switch (expectedPasswordComplexity) {
            case NONE:
                return true;
            case SIMPLE:
                return simpleValidator.validate(new PasswordData(password)).isValid();
            case COMPLEX:
                return complexValidator.validate(new PasswordData(password)).isValid();
            default:
                throw new IllegalStateException("Unexpected password complexity: " + expectedPasswordComplexity);
        }
    }

}
