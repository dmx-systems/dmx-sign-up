package systems.dmx.signup.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfigurationRepositoryTest {

    private final ConfigurationDatasource configurationDatasource = mock();
    private ConfigurationRepository subject = new ConfigurationRepository(configurationDatasource);

    @Test
    @DisplayName("getString() should return value that is not from deprecated property when value available in expected property")
    void getString_should_return_value() {
        // given:
        String key = "foo.bar.baz";
        String value = "Bazubelz";

        String deprecatedKey = "deprecated.foo.bar.baz";

        when(configurationDatasource.getString(key)).thenReturn(value);

        // when:
        ConfigurationRepository.Value result = subject.getString(key, deprecatedKey);

        // then:
        assertThat(result.value).isEqualTo(value);
        assertThat(result.isFromDeprecatedProperty).isFalse();
    }

    @Test
    @DisplayName("getString() should not try to get value from deprecated property when value is available in normal property")
    void getString_should_get_value_from_deprecated_property() {
        // given:
        String key = "foo.bar.baz";
        String value = "Bazubelz";

        String deprecatedKey = "deprecated.foo.bar.baz";

        when(configurationDatasource.getString(any())).thenReturn("other");
        when(configurationDatasource.getString(key)).thenReturn(value);

        // when:
        subject.getString(key, deprecatedKey);

        // then:
        verify(configurationDatasource, times(0)).getString(deprecatedKey);
    }

    @Test
    @DisplayName("getString() should return value that is from deprecated property when value not available in expected property")
    void getString_should_return_value_from_deprecated_property() {
        // given:
        String key = "foo.bar.baz";

        String deprecatedKey = "deprecated.foo.bar.baz";
        String deprecatedValue = "Deprecated Bazubelz";

        when(configurationDatasource.getString(key)).thenReturn(null);
        when(configurationDatasource.getString(any())).thenReturn(deprecatedValue);

        // when:
        ConfigurationRepository.Value result = subject.getString(key, deprecatedKey);

        // then:
        assertThat(result.value).isEqualTo(deprecatedValue);
        assertThat(result.isFromDeprecatedProperty).isFalse();
    }

}
