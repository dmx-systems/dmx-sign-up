package systems.dmx.signup.usecase;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import systems.dmx.signup.configuration.AccountCreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GetAccountCreationPasswordUseCaseTest {

    private GetAccountCreationPasswordUseCase subject = new GetAccountCreationPasswordUseCase();

    @Test
    @DisplayName("invoke() should return provide password when password handling is EDITABLE")
    void invoke_should_return_provided_password() {
        // given:
        String providedPassword = "dhfkdsbihwolkdnkd";

        // when:
        String result = subject.invoke(AccountCreation.PasswordHandling.EDITABLE, providedPassword);

        // then:
        assertThat(result).isEqualTo(providedPassword);
    }

    @Test
    @DisplayName("invoke() should return autogenerate password when password handling is GENERATED")
    void invoke_should_return_generated_password() {
        // given:
        String expectedGeneratedPassword = "dhfkdsbihwolkdnkd";

        try (MockedStatic<RandomStringUtils> mockedStatic = mockStatic(RandomStringUtils.class)) {
            mockedStatic.when(() -> RandomStringUtils.random(anyInt(), anyBoolean(), anyBoolean())).thenReturn(expectedGeneratedPassword);

            // when:
            String result = subject.invoke(AccountCreation.PasswordHandling.GENERATED, "whatever");

            // then:
            mockedStatic.verify(() -> RandomStringUtils.random(
                    GetAccountCreationPasswordUseCase.GENERATED_PASSWORD_LENGTH,
                    GetAccountCreationPasswordUseCase.GENERATED_PASSWORD_USE_LETTERS,
                    GetAccountCreationPasswordUseCase.GENERATED_PASSWORD_USE_NUMBERS));
            assertThat(result).isEqualTo(expectedGeneratedPassword);
        }
    }

}