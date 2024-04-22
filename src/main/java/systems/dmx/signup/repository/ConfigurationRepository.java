package systems.dmx.signup.repository;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConfigurationRepository {

    private final ConfigurationDatasource configurationDatasource;

    @Inject
    ConfigurationRepository(ConfigurationDatasource configurationDatasource) {
        this.configurationDatasource = configurationDatasource;
    }

    public Value getStringWithDefault(String propertyKey, String defaultValue) {
        return new Value(configurationDatasource.getString(propertyKey, defaultValue));
    }

    public Value getString(String propertyKey, String deprecatedPropertyKey) {
        String value = configurationDatasource.getString(propertyKey);
        if (StringUtils.isNotBlank(value)) {
            return new Value(value);
        } else {
            return new Value(configurationDatasource.getString(deprecatedPropertyKey), true);
        }
    }

    public static final class Value {
        public final String value;
        public final boolean isFromDeprecatedProperty;

        public Value(String value) {
            this(value, false);
        }

        public Value(String value, boolean isFromDeprecatedProperty) {
            this.value = value;
            this.isFromDeprecatedProperty = isFromDeprecatedProperty;
        }
    }
}
