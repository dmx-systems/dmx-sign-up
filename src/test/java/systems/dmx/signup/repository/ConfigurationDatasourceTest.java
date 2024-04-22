package systems.dmx.signup.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationDatasourceTest {

    private final ConfigurationDatasource subject = new ConfigurationDatasource();

    @Test
    @DisplayName("getString() should get value from system property")
    void getString_should_get_system_property() {
        // given:
        String givenValue = "dhfsakjhdsdkjsalkdckjbsac";
        String givenProperty = "really.just.some.key.that.most.likely.does.not.exist.as.system.property.before";

        System.setProperty(givenProperty, givenValue);

        // when:
        String result = subject.getString(givenProperty);

        // then:
        assertThat(result).isEqualTo(givenValue);
    }

    @Test
    @DisplayName("getString() should get value from system property with default value")
    void getString_should_get_system_property_with_default_value() {
        // given:
        String givenDefaultValue = "somedefaultvalue";
        String givenProperty = "really.just.some.key.that.most.likely.does.not.exist.as.system.property.before.never.ever";

        // when:
        String result = subject.getString(givenProperty, givenDefaultValue);

        // then:
        assertThat(result).isEqualTo(givenDefaultValue);
    }

}