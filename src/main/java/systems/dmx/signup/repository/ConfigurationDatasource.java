package systems.dmx.signup.repository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConfigurationDatasource {

    @Inject
    ConfigurationDatasource() {
    }

    public String getString(String propertyKey) {
        return System.getProperty(propertyKey);
    }

    public String getString(String propertyKey, String defaultValue) {
        return System.getProperty(propertyKey, defaultValue);
    }

}
