package systems.dmx.signup.mapper;

import systems.dmx.signup.configuration.UsernamePolicy;
import systems.dmx.signup.model.NewAccountData;

import javax.inject.Inject;

public class NewAccountDataMapper {

    @Inject
    public NewAccountDataMapper() {}

    public NewAccountData map(UsernamePolicy policy, String methodName, String username, String emailAddress, String displayName) {
        switch (policy) {
            case UNCONFINED:
                return new NewAccountData(
                        methodName,
                        username,
                        emailAddress,
                        displayName
                );
            case DISPLAYNAME_IS_USERNAME:
                return new NewAccountData(
                        methodName,
                        username,
                        emailAddress,
                        username
                );
            case USERNAME_IS_EMAIL:
                return new NewAccountData(
                        methodName,
                        emailAddress,
                        emailAddress,
                        displayName
                );
        }

        throw new IllegalArgumentException("Invalid arguments");
    }

}
