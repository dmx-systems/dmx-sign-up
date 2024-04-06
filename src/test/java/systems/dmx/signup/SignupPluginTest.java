package systems.dmx.signup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.CoreService;
import systems.dmx.ldap.service.LDAPService;
import systems.dmx.signup.di.DaggerSignupComponent;
import systems.dmx.signup.di.SignupComponent;
import systems.dmx.signup.mapper.IsValidEmailAdressMapper;
import systems.dmx.signup.mapper.NewAccountDataMapper;
import systems.dmx.signup.usecase.GetLdapServiceUseCase;
import systems.dmx.signup.usecase.OptionalService;
import systems.dmx.signup.usecase.ResetPluginMigrationNrUseCase;

import java.lang.reflect.Field;
import java.util.logging.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SignupPluginTest {

    private final CoreService dmx = mock();
    private final AccessControlService accesscontrol = mock();

    private final GetLdapServiceUseCase getLdapServiceUseCase = mock();
    private final IsValidEmailAdressMapper isValidEmailAdressMapper = mock();
    private final NewAccountDataMapper newAccountDataMapper = mock();

    private final ResetPluginMigrationNrUseCase resetPluginMigrationNrUseCase = mock();

    private final SignupPlugin subject = new SignupPlugin();


    @BeforeEach
    public void before() throws NoSuchFieldException, IllegalAccessException {
        // silence logger
        SignupPlugin.logger.setLevel(Level.OFF);

        // DMX transaction
        when(dmx.beginTx()).thenReturn(mock());

        // Manual inject
        set(subject, "dmx", dmx);
        subject.accesscontrol = accesscontrol;
        subject.getLdapServiceUseCase = getLdapServiceUseCase;
        subject.resetPluginMigrationNrUseCase = resetPluginMigrationNrUseCase;
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
            DaggerSignupComponent.Builder builder = mockBuilder();
            staticComponent.when(DaggerSignupComponent::builder).thenReturn(builder);

            // when:
            subject.init();

            // then:
            assertThat(subject.getLdapServiceUseCase).isEqualTo(getLdapServiceUseCase);
            assertThat(subject.isValidEmailAdressMapper).isEqualTo(isValidEmailAdressMapper);
            assertThat(subject.newAccountDataMapper).isEqualTo(newAccountDataMapper);
            assertThat(subject.resetPluginMigrationNrUseCase).isEqualTo(resetPluginMigrationNrUseCase);
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
        when(component.resetPluginMigrationNrUseCase()).thenReturn(resetPluginMigrationNrUseCase);

        DaggerSignupComponent.Builder builder = mock();
        when(builder.coreService(any())).thenReturn(builder);
        when(builder.accessControlService(any())).thenReturn(builder);
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
}
