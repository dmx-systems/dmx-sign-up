package systems.dmx.signup.usecase;

import systems.dmx.ldap.Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.logging.Logger;

import static systems.dmx.signup.configuration.SignUpConfigOptions.*;

@Singleton
public class LogAndVerifyConfigurationUseCase {
    static final Logger logger = Logger.getLogger(LogAndVerifyConfigurationUseCase.class.getName());

    @Inject
    LogAndVerifyConfigurationUseCase() {
    }

    public void invoke(Configuration ldapConfiguration, List<String> authorizationMethods) {
        logger.info("\n  dmx.signup.account_creation: " + CONFIG_ACCOUNT_CREATION + "\n"
                + "  dmx.signup.account_creation_password_handling: " + CONFIG_ACCOUNT_CREATION_PASSWORD_HANDLING + "\n"
                + "  dmx.signup.username_policy: " + CONFIG_USERNAME_POLICY + "\n"
                + "  dmx.signup.confirm_email_address: " + CONFIG_EMAIL_CONFIRMATION + "\n"
                + "  dmx.signup.admin_mailbox: " + CONFIG_ADMIN_MAILBOX + "\n"
                + "  dmx.signup.system_mailbox: " + CONFIG_FROM_MAILBOX + "\n"
                + "  dmx.signup.ldap_account_creation: " + CONFIG_CREATE_LDAP_ACCOUNTS + "\n"
                + "  dmx.signup.account_creation_auth_ws_uri: " + CONFIG_ACCOUNT_CREATION_AUTH_WS_URI + "\n"
                + "  dmx.signup.restrict_auth_methods: " + CONFIG_RESTRICT_AUTH_METHODS + "\n"
                + "  dmx.signup.token_expiration_time: " + CONFIG_TOKEN_EXPIRATION_DURATION.toHours() + "\n"
                + "  dmx.signup.expected_password_complexity: " + CONFIG_EXPECTED_PASSWORD_COMPLEXITY + "\n"

        );
        logger.info("Available auth methods and order:" + authorizationMethods + "\n");
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

        if (CONFIG_ADMIN_MAILBOX == null || CONFIG_ADMIN_MAILBOX.isEmpty()) {
            logger.warning("'dmx.signup.admin_mailbox' is not configured. Please correct this otherwise various notification emails cannot be send.");
        }
    }
}
