package systems.dmx.signup.usecase;

import org.apache.commons.lang3.StringUtils;
import systems.dmx.signup.configuration.Configuration;
import systems.dmx.signup.repository.ConfigurationRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.logging.Logger;

import static systems.dmx.signup.configuration.SignUpConfigOptions.*;

@Singleton
public class LogAndVerifyConfigurationUseCase {
    static final Logger logger = Logger.getLogger(LogAndVerifyConfigurationUseCase.class.getName());

    private final ConfigurationRepository configurationRepository;

    @Inject
    LogAndVerifyConfigurationUseCase(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    private String getDeprecatedPropertyWarning(String deprecatedPropertyKey, String propertyKey) {
        return String.format("A configuration value from the deprecated property '%s' was used. Please migrate the value to the property '%s'.", deprecatedPropertyKey, propertyKey);
    }

    public Configuration invoke(List<String> authorizationMethods) {
        ConfigurationRepository.Value adminEmailAddress = configurationRepository.getString(Keys.SYSTEM_ADMIN_MAILBOX, Keys.DEPRECATED_ADMIN_MAILBOX);
        ConfigurationRepository.Value fromEmailAddress = configurationRepository.getString(Keys.SYSTEM_FROM_MAILBOX, Keys.DEPRECATED_FROM_MAILBOX);
        ConfigurationRepository.Value fromName = configurationRepository.getStringWithDefault(Keys.SYSTEM_FROM_NAME, "");

        logger.info("\n  dmx.signup.account_creation: " + CONFIG_ACCOUNT_CREATION + "\n"
                + "  dmx.signup.account_creation_password_handling: " + CONFIG_ACCOUNT_CREATION_PASSWORD_HANDLING + "\n"
                + "  dmx.signup.username_policy: " + CONFIG_USERNAME_POLICY + "\n"
                + "  dmx.signup.confirm_email_address: " + CONFIG_EMAIL_CONFIRMATION + "\n"
                + "  dmx.signup.system_admin_mailbox: " + adminEmailAddress.value + "\n"
                + "  dmx.signup.system_from_mailbox: " + fromEmailAddress.value + "\n"
                + "  dmx.signup.system_from_name: " + fromName.value + "\n"
                + "  dmx.signup.account_creation_auth_ws_uri: " + CONFIG_ACCOUNT_CREATION_AUTH_WS_URI + "\n"
                + "  dmx.signup.restrict_auth_methods: " + CONFIG_RESTRICT_AUTH_METHODS + "\n"
                + "  dmx.signup.token_expiration_time: " + CONFIG_TOKEN_EXPIRATION_DURATION.toHours() + "\n"
                + "  dmx.signup.expected_password_complexity: " + CONFIG_EXPECTED_PASSWORD_COMPLEXITY + "\n"
        );

        logger.info("Available auth methods and order:" + authorizationMethods + "\n");
        /* Those warnings should go into LDAP plugin instead
        if (ldapConfiguration == null) {
            if (CONFIG_CREATE_LDAP_ACCOUNTS) {
                logger.warning("LDAP Account creation configured but respective plugin not available!");
            }
        } else if (!ldapConfiguration.useBindAccount) {
            logger.info("LDAP is configured to not use bind account. Only log-in operation will work.");
            if (CONFIG_CREATE_LDAP_ACCOUNTS) {
                logger.warning("LDAP Account creation configured but no bind account should be used. Enable and provide a bind account otherwise account creation cannot work.");
            }
        }
        */

        if (StringUtils.isBlank(adminEmailAddress.value)) {
            logger.warning("'dmx.signup.system_admin_mailbox' is not configured. Please correct this otherwise various notification emails cannot be send.");
        }

        if (adminEmailAddress.isFromDeprecatedProperty) {
            logger.warning(getDeprecatedPropertyWarning(Keys.DEPRECATED_ADMIN_MAILBOX, Keys.SYSTEM_ADMIN_MAILBOX));
        }

        if (fromEmailAddress.isFromDeprecatedProperty) {
            logger.warning(getDeprecatedPropertyWarning(Keys.DEPRECATED_FROM_MAILBOX, Keys.SYSTEM_FROM_MAILBOX));
        }

        return new Configuration(
                CONFIG_ACCOUNT_CREATION,
                CONFIG_ACCOUNT_CREATION_PASSWORD_HANDLING,
                CONFIG_EXPECTED_PASSWORD_COMPLEXITY,
                CONFIG_EXPECTED_MIN_PASSWORD_LENGTH,
                CONFIG_EXPECTED_MAX_PASSWORD_LENGTH,
                CONFIG_USERNAME_POLICY,
                CONFIG_EMAIL_CONFIRMATION,
                adminEmailAddress.value,
                fromEmailAddress.value,
                fromName.value,
                CONFIG_ACCOUNT_CREATION_AUTH_WS_URI,
                CONFIG_RESTRICT_AUTH_METHODS,
                CONFIG_TOKEN_EXPIRATION_DURATION
        );

    }
}
