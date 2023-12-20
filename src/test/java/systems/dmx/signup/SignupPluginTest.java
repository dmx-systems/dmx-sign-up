package systems.dmx.signup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.ldap.service.LDAPService;
import systems.dmx.signup.usecase.GetLdapServiceUseCase;
import systems.dmx.signup.usecase.OptionalService;

import java.util.logging.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SignupPluginTest {

    private final AccessControlService accesscontrol = mock();

    private final GetLdapServiceUseCase getLdapServiceUseCase = mock();

    private final SignupPlugin subject = new SignupPlugin();
    

    @BeforeEach
    public void before() {
        // silence logger
        SignupPlugin.logger.setLevel(Level.OFF);

        // Manual inject
        subject.getLdapServiceUseCase = getLdapServiceUseCase;
        subject.accesscontrol = accesscontrol;
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