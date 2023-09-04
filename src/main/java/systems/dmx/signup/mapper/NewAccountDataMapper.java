package systems.dmx.signup.mapper;

import systems.dmx.signup.configuration.UsernamePolicy;
import systems.dmx.signup.model.NewAccountData;

public class NewAccountDataMapper {

    public NewAccountData map(UsernamePolicy policy, String username, String email, String displayName) {
        switch (policy) {
            case UNCONFINED:
                return new NewAccountData(
                        username,
                        email,
                        displayName
                );
            case USERNAME_IS_DISPLAYNAME:
                return new NewAccountData(
                        username,
                        email,
                        username
                );
            case USERNAME_IS_EMAIL:
                return new NewAccountData(
                        email,
                        email,
                        displayName
                );
        }

        throw new IllegalArgumentException("Invalid arguments");
    }

}
