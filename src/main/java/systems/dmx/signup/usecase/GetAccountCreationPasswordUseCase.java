package systems.dmx.signup.usecase;

import org.apache.commons.lang3.RandomStringUtils;
import systems.dmx.signup.configuration.AccountCreation;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GetAccountCreationPasswordUseCase {

    public static final int GENERATED_PASSWORD_LENGTH = 23;
    public static final boolean GENERATED_PASSWORD_USE_LETTERS = true;

    public static final boolean GENERATED_PASSWORD_USE_NUMBERS = true;

    @Inject
    GetAccountCreationPasswordUseCase() {}

    public String invoke(AccountCreation.PasswordHandling passwordHandling, String providedPassword) {
        switch (passwordHandling) {
            case EDITABLE:
                return providedPassword;
            case GENERATED:
                return RandomStringUtils.random(GENERATED_PASSWORD_LENGTH, GENERATED_PASSWORD_USE_LETTERS, GENERATED_PASSWORD_USE_NUMBERS);
            default:
                throw new IllegalStateException("Unexpected case");
        }
    }

}
