package systems.dmx.signup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.CoreService;
import systems.dmx.ldap.service.LDAPService;
import systems.dmx.signup.configuration.AccountCreation;
import systems.dmx.signup.di.DaggerSignupComponent;
import systems.dmx.signup.di.SignupComponent;
import systems.dmx.signup.mapper.IsValidEmailAdressMapper;
import systems.dmx.signup.mapper.NewAccountDataMapper;
import systems.dmx.signup.usecase.*;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static systems.dmx.signup.SignUpRequestResult.Code.*;

class SignupPluginTest {

    private final CoreService dmx = mock();
    private final AccessControlService accesscontrol = mock();

    private final GetLdapServiceUseCase getLdapServiceUseCase = mock();
    private final IsValidEmailAdressMapper isValidEmailAdressMapper = mock();
    private final NewAccountDataMapper newAccountDataMapper = mock();

    private final GetAccountCreationPasswordUseCase getAccountCreationPasswordUseCase = mock();

    private final HasAccountCreationPrivilegeUseCase hasAccountCreationPrivilegeUseCase = mock();

    private final IsPasswordComplexEnoughUseCase isPasswordComplexEnoughUseCase = mock();

    private final LogAndVerifyConfigurationUseCase logAndVerifyConfigurationUseCase = mock();

    private final SignupPlugin subject = new SignupPlugin();


    @BeforeEach
    public void before() throws NoSuchFieldException, IllegalAccessException {
        // silence logger
        SignupPlugin.logger.setLevel(Level.OFF);

        // Manual inject
        set(subject, "dmx", dmx);
        subject.accesscontrol = accesscontrol;
        subject.getLdapServiceUseCase = getLdapServiceUseCase;
        subject.getAccountCreationPasswordUseCase = getAccountCreationPasswordUseCase;
        subject.hasAccountCreationPrivilegeUseCase = hasAccountCreationPrivilegeUseCase;
        subject.isValidEmailAdressMapper = isValidEmailAdressMapper;
        subject.newAccountDataMapper = newAccountDataMapper;
        subject.isPasswordComplexEnoughUseCase = isPasswordComplexEnoughUseCase;
        subject.logAndVerifyConfigurationUseCase = logAndVerifyConfigurationUseCase;
    }

    private void set(Object o, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = PluginActivator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(o, value);

    }

    @Test
    @DisplayName("init() should run dependency injection")
    void init_should_run_di() {
        try (MockedStatic<DaggerSignupComponent> staticComponent = mockStatic(DaggerSignupComponent.class)) {
            // given:
            DaggerSignupComponent.Builder builder = mockBuilder();
            staticComponent.when(DaggerSignupComponent::builder).thenReturn(builder);

            // when:
            subject.init();

            // then:
            verify(builder).accessControlService(accesscontrol);
            verify(builder).coreService(dmx);
            verify(builder).build();
        }
    }

    @Test
    @DisplayName("init() should set dependencies")
    void init_should_set_dependencies() {
        try (MockedStatic<DaggerSignupComponent> staticComponent = mockStatic(DaggerSignupComponent.class)) {
            // given:
            // reset what beforeAll does
            subject.getLdapServiceUseCase = null;
            subject.newAccountDataMapper = null;
            subject.getAccountCreationPasswordUseCase = null;
            subject.hasAccountCreationPrivilegeUseCase = null;
            subject.isValidEmailAdressMapper = null;
            subject.isPasswordComplexEnoughUseCase = null;
            subject.logAndVerifyConfigurationUseCase = null;

            DaggerSignupComponent.Builder builder = mockBuilder();
            staticComponent.when(DaggerSignupComponent::builder).thenReturn(builder);

            // when:
            subject.init();

            // then:
            assertThat(subject.getLdapServiceUseCase).isEqualTo(getLdapServiceUseCase);
            assertThat(subject.isValidEmailAdressMapper).isEqualTo(isValidEmailAdressMapper);
            assertThat(subject.newAccountDataMapper).isEqualTo(newAccountDataMapper);
            assertThat(subject.getAccountCreationPasswordUseCase).isEqualTo(getAccountCreationPasswordUseCase);
            assertThat(subject.hasAccountCreationPrivilegeUseCase).isEqualTo(hasAccountCreationPrivilegeUseCase);
            assertThat(subject.isPasswordComplexEnoughUseCase).isEqualTo(isPasswordComplexEnoughUseCase);
            assertThat(subject.logAndVerifyConfigurationUseCase).isEqualTo(logAndVerifyConfigurationUseCase);
        }
    }

    /**
     * Mocks the complete {@link systems.dmx.signup.di.DaggerSignupComponent.Builder}
     *
     * @return
     */
    private DaggerSignupComponent.Builder mockBuilder() {
        // Sets up SignupComponent
        SignupComponent component = mock();
        when(component.getLdapServiceUseCase()).thenReturn(getLdapServiceUseCase);
        when(component.isValidEmailAdressMapper()).thenReturn(isValidEmailAdressMapper);
        when(component.newAccountDataMapper()).thenReturn(newAccountDataMapper);
        when(component.getAccountCreationPasswordUseCase()).thenReturn(getAccountCreationPasswordUseCase);
        when(component.hasAccountCreationPrivilegeUseCase()).thenReturn(hasAccountCreationPrivilegeUseCase);
        when(component.isPasswordComplexEnoughUseCase()).thenReturn(isPasswordComplexEnoughUseCase);
        when(component.logAndVerifyConfigurationUseCase()).thenReturn(logAndVerifyConfigurationUseCase);

        DaggerSignupComponent.Builder builder = mock();
        when(builder.coreService(any())).thenReturn(builder);
        when(builder.accessControlService(any())).thenReturn(builder);
        when(builder.workspacesService(any())).thenReturn(builder);
        when(builder.build()).thenReturn(component);

        return builder;
    }

    @Test
    @DisplayName("allPluginsActive() should set optional LDAP service")
    void allPluginsActive_should_set_optional_ldap_service() {
        // given:
        OptionalService<LDAPService> optionalService = mock();
        when(getLdapServiceUseCase.invoke(any())).thenReturn(optionalService);

        // when:
        subject.allPluginsActive();

        // then:
        verify(getLdapServiceUseCase).invoke(any());
        assertThat(subject.ldap).isEqualTo(optionalService);
    }

    @Test
    @DisplayName("isPasswordComplexEnough() should call IsPasswordComplexEnoughUseCase and return its value")
    void isPasswordComplexEnough_should_call_IsPasswordComplexEnoughUseCase() {
        // given:
        String givenPassword = "passwurst";
        when(isPasswordComplexEnoughUseCase.invoke(any(), any())).thenReturn(true);

        // when:
        Boolean result = subject.isPasswordComplexEnough(givenPassword);

        // then:
        verify(isPasswordComplexEnoughUseCase).invoke(any(), eq(givenPassword));
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("requestSignUp() should return result with ACCOUNT_CREATION_DENIED when self registration is not enabled")
    void requestSignUp_should_deny_when_no_self_registration() {
        // given:
        String givenUsername = "username";
        String givenEmailAddress = "email@address.org";
        String givenDisplayName = "display name";
        String givenPassword = "12345678";
        boolean givenSkipConfirmation = false;
        try (MockedStatic<AccountCreation> mockedStatic = mockStatic(AccountCreation.class)) {
            mockedStatic.when(() -> AccountCreation.fromStringOrDisabled(anyString())).thenReturn(AccountCreation.DISABLED);

            // when:
            SignUpRequestResult result = subject.requestSignUp(givenUsername, givenEmailAddress, givenDisplayName, givenPassword, givenSkipConfirmation);

            // then:
            assertThat(result.code).isEqualTo(ACCOUNT_CREATION_DENIED);
        }
    }

    @Test
    @DisplayName("requestSignUp() should return result with ACCOUNT_CREATION_DENIED when self registration is enabled for admin but user has no account creation privilege")
    void requestSignUp_should_deny_when_no_account_creation_privilege() {
        // given:
        when(hasAccountCreationPrivilegeUseCase.invoke()).thenReturn(false);

        String givenUsername = "username";
        String givenEmailAddress = "email@address.org";
        String givenDisplayName = "display name";
        String givenPassword = "12345678";
        boolean givenSkipConfirmation = false;
        try (MockedStatic<AccountCreation> mockedStatic = mockStatic(AccountCreation.class)) {
            mockedStatic.when(() -> AccountCreation.fromStringOrDisabled(anyString())).thenReturn(AccountCreation.ADMIN);

            // when:
            SignUpRequestResult result = subject.requestSignUp(givenUsername, givenEmailAddress, givenDisplayName, givenPassword, givenSkipConfirmation);

            // then:
            assertThat(result.code).isEqualTo(ACCOUNT_CREATION_DENIED);
        }
    }

    @Test
    @DisplayName("requestSignUp() should return result with ERROR_INVALID_EMAIL when when email is not valid")
    void requestSignUp_should_deny_when_email_invalid() {
        // given:
        when(hasAccountCreationPrivilegeUseCase.invoke()).thenReturn(true);
        when(isValidEmailAdressMapper.map(anyString())).thenReturn(false);

        String givenUsername = "username";
        String givenEmailAddress = "email@address.org";
        String givenDisplayName = "display name";
        String givenPassword = "12345678";
        boolean givenSkipConfirmation = false;

        try (MockedStatic<AccountCreation> mockedStatic = mockStatic(AccountCreation.class)) {
            mockedStatic.when(() -> AccountCreation.fromStringOrDisabled(anyString())).thenReturn(AccountCreation.PUBLIC);

            // when:
            SignUpRequestResult result = subject.requestSignUp(givenUsername, givenEmailAddress, givenDisplayName, givenPassword, givenSkipConfirmation);

            // then:
            verify(isValidEmailAdressMapper).map(givenEmailAddress);
            assertThat(result.code).isEqualTo(ERROR_INVALID_EMAIL);
        }
    }

    @Test
    @DisplayName("requestSignUp() should return result with ERROR_PASSWORD_COMPLEXITY_INSUFFICIENT when password complexity check fails")
    void requestSignUp_should_deny_when_password_not_strong() {
        // given:
        when(isPasswordComplexEnoughUseCase.invoke(any(), anyString())).thenReturn(false);

        String givenUsername = "username";
        String givenEmailAddress = "email@address.org";
        String givenDisplayName = "display name";
        String givenPassword = "12345678";
        boolean givenSkipConfirmation = false;

        when(hasAccountCreationPrivilegeUseCase.invoke()).thenReturn(true);
        when(isValidEmailAdressMapper.map(anyString())).thenReturn(true);
        when(getAccountCreationPasswordUseCase.invoke(any(), any())).thenReturn(givenPassword);

        try (MockedStatic<AccountCreation> mockedStatic = mockStatic(AccountCreation.class)) {
            mockedStatic.when(() -> AccountCreation.fromStringOrDisabled(anyString())).thenReturn(AccountCreation.PUBLIC);

            // when:
            SignUpRequestResult result = subject.requestSignUp(givenUsername, givenEmailAddress, givenDisplayName, givenPassword, givenSkipConfirmation);

            // then:
            verify(isPasswordComplexEnoughUseCase).invoke(any(), eq(givenPassword));
            assertThat(result.code).isEqualTo(ERROR_PASSWORD_COMPLEXITY_INSUFFICIENT);
        }
    }

    @Test
    @DisplayName("requestSignUp() should not check password when password generated")
    void requestSignUp_should_not_check_password_when_generated() {
        // given:
        when(isPasswordComplexEnoughUseCase.invoke(any(), anyString())).thenReturn(false);

        String givenUsername = "username";
        String givenEmailAddress = "email@address.org";
        String givenDisplayName = "display name";
        String givenPassword = "12345678";
        boolean givenSkipConfirmation = false;

        when(hasAccountCreationPrivilegeUseCase.invoke()).thenReturn(true);
        when(isValidEmailAdressMapper.map(anyString())).thenReturn(true);
        when(getAccountCreationPasswordUseCase.invoke(any(), any())).thenReturn("some generated password that is different from the given one");

        try (MockedStatic<AccountCreation> mockedStatic = mockStatic(AccountCreation.class)) {
            mockedStatic.when(() -> AccountCreation.fromStringOrDisabled(anyString())).thenReturn(AccountCreation.PUBLIC);

            // when:
            subject.requestSignUp(givenUsername, givenEmailAddress, givenDisplayName, givenPassword, givenSkipConfirmation);

            // then:
            verifyNoInteractions(isPasswordComplexEnoughUseCase);
        }
    }

    private static Stream<Arguments> passwordHandlingParams() {
        return Stream.of(
                Arguments.of(AccountCreation.PasswordHandling.GENERATED),
                Arguments.of(AccountCreation.PasswordHandling.EDITABLE)
        );
    }

    @ParameterizedTest(name = "{displayName} with configured account creation handling {0} and password ")
    @MethodSource("passwordHandlingParams")
    @DisplayName("requestSignUp() should call get account creation password")
    void requestSignUp_should_call_GetAccountCreationPasswordUseCase(AccountCreation.PasswordHandling givenPasswordHandling) {
        // given:
        when(hasAccountCreationPrivilegeUseCase.invoke()).thenReturn(true);
        when(isValidEmailAdressMapper.map(anyString())).thenReturn(true);

        String givenUsername = "username";
        String givenEmailAddress = "email@address.org";
        String givenDisplayName = "display name";
        String givenPassword = "12345678";
        boolean givenSkipConfirmation = false;

        try (MockedStatic<AccountCreation> mockedStatic = mockStatic(AccountCreation.class);
             MockedStatic<AccountCreation.PasswordHandling> mockedStatic2 = mockStatic(AccountCreation.PasswordHandling.class)) {
            mockedStatic.when(() -> AccountCreation.fromStringOrDisabled(anyString())).thenReturn(AccountCreation.PUBLIC);
            mockedStatic2.when(() -> AccountCreation.PasswordHandling.fromStringOrEditable(anyString())).thenReturn(givenPasswordHandling);

            // when:
            subject.requestSignUp(givenUsername, givenEmailAddress, givenDisplayName, givenPassword, givenSkipConfirmation);

            // then:
            // first parameter should actually be givenPasswordHandling. Somehow the static mocking does not work properly
            verify(getAccountCreationPasswordUseCase).invoke(any(), matches(givenPassword));
        }
    }

    // TODO: Test that password complexity is enforced password change
}