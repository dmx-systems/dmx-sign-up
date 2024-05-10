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
    private final static String methodName = "FooBaz";
    private final static String username = "user";
    private final static String emailAddress = "email@address";
    private final static String displayName = "Usi User";

    private static Arguments args(
            UsernamePolicy policy,
            String expectedMethodName,
            String expectedUsername,
            String expectedEmailAddress,
            String expectedDisplayName) {
        return Arguments.of(policy, expectedMethodName, expectedUsername, expectedEmailAddress, expectedDisplayName);
    }

    private static Stream<Arguments> policyAndExpectedValues() {
        return Stream.of(
                args(UsernamePolicy.UNCONFINED, methodName, username, emailAddress, displayName),
                args(UsernamePolicy.USERNAME_IS_EMAIL, methodName, emailAddress, emailAddress, displayName),
                args(UsernamePolicy.DISPLAYNAME_IS_USERNAME, methodName, username, emailAddress, username)
        );
    }

    @ParameterizedTest(name = "with method name '{1}', username '{2}', email address '{3}' and display name '{4}'")
    @MethodSource("policyAndExpectedValues")
    @DisplayName("map() should return new account data")
    void map_should_return_correct_account_data(
            UsernamePolicy givenPolicy,
            String expectedMethodName,
            String expectedUsername,
            String expectedEmailAddress,
            String expectedDisplayName
    ) {
        // when:
        NewAccountData result = subject.map(givenPolicy, methodName, username, emailAddress, displayName);

        // then:
        assertThat(result).isNotNull();
        assertThat(result.methodName).isEqualTo(expectedMethodName);
        assertThat(result.username).isEqualTo(expectedUsername);
        assertThat(result.emailAddress).isEqualTo(expectedEmailAddress);
        assertThat(result.displayName).isEqualTo(expectedDisplayName);
    }

}