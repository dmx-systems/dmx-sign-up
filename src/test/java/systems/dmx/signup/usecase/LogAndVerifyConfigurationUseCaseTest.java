package systems.dmx.signup.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import systems.dmx.signup.configuration.Configuration;
import systems.dmx.signup.configuration.SignUpConfigOptions.Keys;
import systems.dmx.signup.repository.ConfigurationRepository;

import java.util.logging.Level;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LogAndVerifyConfigurationUseCaseTest {

    private final ConfigurationRepository configurationRepository = mock();

    private final LogAndVerifyConfigurationUseCase subject = new LogAndVerifyConfigurationUseCase(configurationRepository);

    @BeforeEach
    void beforeEach() {
        LogAndVerifyConfigurationUseCase.logger.setLevel(Level.OFF);

        // by default return configuration value mocks for all keys
        when(configurationRepository.getString(any(), any())).thenReturn(mock());
        when(configurationRepository.getStringWithDefault(any(), any())).thenReturn(mock());
    }

    @Test
    @DisplayName("invoke() should return Configuration with admin email address set from 'dmx.signup.system_admin_mailbox' property")
    void invoke_should_return_configuration_with_admin_email_address() {
        // given:
        ConfigurationRepository.Value adminEmailAddress = new ConfigurationRepository.Value("admin@email", false);
        when(configurationRepository.getString(eq(Keys.SYSTEM_ADMIN_MAILBOX), any())).thenReturn(adminEmailAddress);

        // when:
        Configuration result = subject.invoke(emptyList());

        // then:
        assertThat(result.adminEmailAddress).isEqualTo(adminEmailAddress.value);
        verify(configurationRepository).getString(eq(Keys.SYSTEM_ADMIN_MAILBOX), any());
    }

    @Test
    @DisplayName("invoke() should return Configuration with admin email address set from 'dmx.signup.admin_mailbox' deprecated property")
    void invoke_should_return_configuration_with_admin_email_address_from_deprecated_property() {
        // given:
        ConfigurationRepository.Value adminEmailAddress = new ConfigurationRepository.Value("admin@email", true);
        when(configurationRepository.getString(any(), eq(Keys.DEPRECATED_ADMIN_MAILBOX))).thenReturn(adminEmailAddress);

        // when:
        Configuration result = subject.invoke(emptyList());

        // then:
        assertThat(result.adminEmailAddress).isEqualTo(adminEmailAddress.value);
        verify(configurationRepository).getString(any(), eq(Keys.DEPRECATED_ADMIN_MAILBOX));
    }

    @Test
    @DisplayName("invoke() should return Configuration with 'from' email address set from 'dmx.signup.system_from_mailbox' property")
    void invoke_should_return_configuration_with_from_email_address() {
        // given:
        ConfigurationRepository.Value fromEmailAddress = new ConfigurationRepository.Value("from@email", false);
        when(configurationRepository.getString(eq(Keys.SYSTEM_FROM_MAILBOX), any())).thenReturn(fromEmailAddress);

        // when:
        Configuration result = subject.invoke(emptyList());

        // then:
        assertThat(result.fromEmailAddress).isEqualTo(fromEmailAddress.value);
        verify(configurationRepository).getString(eq(Keys.SYSTEM_FROM_MAILBOX), any());
    }

    @Test
    @DisplayName("invoke() should return Configuration with 'from' email address set from 'dmx.signup.system_mailbox' deprecated property")
    void invoke_should_return_configuration_with_from_email_address_from_deprecated_property() {
        // given:
        ConfigurationRepository.Value fromEmailAddress = new ConfigurationRepository.Value("from@email", true);
        when(configurationRepository.getString(any(), eq(Keys.DEPRECATED_FROM_MAILBOX))).thenReturn(fromEmailAddress);

        // when:
        Configuration result = subject.invoke(emptyList());

        // then:
        assertThat(result.fromEmailAddress).isEqualTo(fromEmailAddress.value);
        verify(configurationRepository).getString(any(), eq(Keys.DEPRECATED_FROM_MAILBOX));
    }

    @Test
    @DisplayName("invoke() should return Configuration with 'from' name set from 'dmx.signup.system_from_name' property")
    void invoke_should_return_configuration_with_from_name() {
        // given:
        ConfigurationRepository.Value fromName = new ConfigurationRepository.Value("Mr. From", false);
        when(configurationRepository.getStringWithDefault(eq(Keys.SYSTEM_FROM_NAME), any())).thenReturn(fromName);

        // when:
        Configuration result = subject.invoke(emptyList());

        // then:
        assertThat(result.fromName).isEqualTo(fromName.value);
        verify(configurationRepository).getStringWithDefault(eq(Keys.SYSTEM_FROM_NAME), eq(""));
    }
}