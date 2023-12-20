package systems.dmx.signup.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import systems.dmx.signup.configuration.UsernamePolicy;
import systems.dmx.signup.model.NewAccountData;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NewAccountDataMapperTest {

    private final NewAccountDataMapper subject = new NewAccountDataMapper();
    private final static String username = "user";
    private final static String emailAddress = "email@address";
    private final static String displayName = "Usi User";

    private static Arguments args(
            UsernamePolicy policy,
            String expectedUsername,
            String expectedEmailAddress,
            String expectedDisplayName) {
        return Arguments.of(policy, expectedUsername, expectedEmailAddress, expectedDisplayName);
    }

    private static Stream<Arguments> policyAndExpectedValues() {
        return Stream.of(
                args(UsernamePolicy.UNCONFINED, username, emailAddress, displayName),
                args(UsernamePolicy.USERNAME_IS_EMAIL, emailAddress, emailAddress, displayName),
                args(UsernamePolicy.DISPLAYNAME_IS_USERNAME_, username, emailAddress, username)
        );
    }

    @ParameterizedTest(name = "with username '{1}', email address '{2}' and display name '{3}'")
    @MethodSource("policyAndExpectedValues")
    @DisplayName("map() should return new account data")
    void map_should_return_correct_account_data(
            UsernamePolicy givenPolicy,
            String expectedUsername,
            String expectedEmailAddress,
            String expectedDisplayName
    ) {
        // when:
        NewAccountData result = subject.map(givenPolicy, username, emailAddress, displayName);

        // then:
        assertThat(result).isNotNull();
        assertThat(result.username).isEqualTo(expectedUsername);
        assertThat(result.emailAddress).isEqualTo(expectedEmailAddress);
        assertThat(result.displayName).isEqualTo(expectedDisplayName);
    }

}