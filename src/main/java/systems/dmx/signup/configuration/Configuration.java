package systems.dmx.signup.configuration;

import java.time.Duration;

/**
 * Static configuration values from DMX' config.properties.
 */
public class Configuration {

    public final AccountCreation accountCreation;

    public final AccountCreation.PasswordHandling accountCreationPasswordHandling;

    public final ExpectedPasswordComplexity expectedPasswordComplexity;

    public final int expectedMinPasswordLength;

    public final int expectedMaxPasswordLength;

    public final UsernamePolicy usernamePolicy;

    public final boolean emailConfirmationEnabled;

    public final String adminEmailAddress;

    public final String fromEmailAddress;

    public final String fromName;

    public final String accountCreationAuthWorkspaceUri;

    public final String restrictAuthMethods;

    public final Duration tokenExpirationDuration;

    public Configuration(AccountCreation accountCreation, AccountCreation.PasswordHandling accountCreationPasswordHandling, ExpectedPasswordComplexity expectedPasswordComplexity, int expectedMinPasswordLength, int expectedMaxPasswordLength, UsernamePolicy usernamePolicy, boolean emailConfirmationEnabled, String adminEmailAddress, String fromEmailAddress, String fromName, String accountCreationAuthWorkspaceUri, String restrictAuthMethods, Duration tokenExpirationDuration) {
        this.accountCreation = accountCreation;
        this.accountCreationPasswordHandling = accountCreationPasswordHandling;
        this.expectedPasswordComplexity = expectedPasswordComplexity;
        this.expectedMinPasswordLength = expectedMinPasswordLength;
        this.expectedMaxPasswordLength = expectedMaxPasswordLength;
        this.usernamePolicy = usernamePolicy;
        this.emailConfirmationEnabled = emailConfirmationEnabled;
        this.adminEmailAddress = adminEmailAddress;
        this.fromEmailAddress = fromEmailAddress;
        this.fromName = fromName;
        this.accountCreationAuthWorkspaceUri = accountCreationAuthWorkspaceUri;
        this.restrictAuthMethods = restrictAuthMethods;
        this.tokenExpirationDuration = tokenExpirationDuration;
    }

    /**
     * {@link AccountCreation}
     */
    public AccountCreation getAccountCreation() {
        return accountCreation;
    }

    /**
     * {@link AccountCreation.PasswordHandling}
     */
    public AccountCreation.PasswordHandling getAccountCreationPasswordHandling() {
        return accountCreationPasswordHandling;
    }

    /**
     * {@link ExpectedPasswordComplexity}
     */
    public ExpectedPasswordComplexity getExpectedPasswordComplexity() {
        return expectedPasswordComplexity;
    }

    /**
     * Minimum number of characters for a password to be accepted.
     */
    public int getExpectedMinPasswordLength() {
        return expectedMinPasswordLength;
    }

    /**
     * Maximum number of characters for a password to be accepted.
     */
    public int getExpectedMaxPasswordLength() {
        return expectedMaxPasswordLength;
    }

    /**
     * {@link UsernamePolicy}
     */
    public UsernamePolicy getUsernamePolicy() {
        return usernamePolicy;
    }

    /**
     * Toggle whether an email address needs to be confirmed by following a link that is in the email.
     */
    public boolean isEmailConfirmationEnabled() {
        return emailConfirmationEnabled;
    }

    /**
     * Email address of the admin user
     */
    public String getAdminEmailAddress() {
        return adminEmailAddress;
    }

    /**
     * Email address that appears in "from" in all automated emails.
     */
    public String getFromEmailAddress() {
        return fromEmailAddress;
    }

    /**
     * Name that appears in "from" in all automated emails.
     */
    public String getFromName() {
        return fromName;
    }

    /**
     * URI of the workspace that is used for checking whether a user has account creation privilege.
     */
    public String getAccountCreationAuthWorkspaceUri() {
        return accountCreationAuthWorkspaceUri;
    }

    /**
     * List and order of which auth methods are available.
     */
    public String getRestrictAuthMethods() {
        return restrictAuthMethods;
    }

    /**
     * Duration after which account creation or password change tokens expire when not used.
     */
    public Duration getTokenExpirationDuration() {
        return tokenExpirationDuration;
    }
}
