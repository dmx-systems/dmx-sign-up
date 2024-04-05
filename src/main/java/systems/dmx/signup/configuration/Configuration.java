package systems.dmx.signup.configuration;

import java.time.Duration;

/**
 * Static configuration values from DMX' config.properties.
 */
public class Configuration {

    /**
     * {@link AccountCreation}
     */
    public final AccountCreation accountCreation;

    /**
     * {@link AccountCreation.PasswordHandling}
     */
    public final AccountCreation.PasswordHandling accountCreationPasswordHandling;

    /**
     * {@link ExpectedPasswordComplexity}
     */
    public final ExpectedPasswordComplexity expectedPasswordComplexity;

    /**
     * Minimum number of characters for a password to be accepted.
     */
    public final int expectedMinPasswordLength;

    /**
     * Maximum number of characters for a password to be accepted.
     */
    public final int expectedMaxPasswordLength;

    /**
     * {@link UsernamePolicy}
     */
    public final UsernamePolicy usernamePolicy;

    /**
     * Toggle whether an email address needs to be confirmed by following a link that is in the email.
     */
    public final boolean emailConfirmationEnabled;

    /**
     * Email address of the admin user
     */
    public final String adminEmailAddress;

    /**
     * Email address that appears in "from" in all automated emails.
     */
    public final String fromEmailAddress;

    /**
     * Toggle whether new users are created in LDAP.
     */
    public final boolean createLdapAccountsEnabled;

    /**
     * URI of the workspace that is used for checking whether a user has account creation privilege.
     */
    public final String accountCreationAuthWorkspaceUri;

    /**
     * List and order of which auth methods are available.
     */
    public final String restrictAuthMethods;

    /**
     * Duration after which account creation or password change tokens expire when not used.
     */
    public final Duration tokenExpirationDuration;

    public Configuration(AccountCreation accountCreation, AccountCreation.PasswordHandling accountCreationPasswordHandling, ExpectedPasswordComplexity expectedPasswordComplexity, int expectedMinPasswordLength, int expectedMaxPasswordLength, UsernamePolicy usernamePolicy, boolean emailConfirmationEnabled, String adminEmailAddress, String fromEmailAddress, boolean createLdapAccountsEnabled, String accountCreationAuthWorkspaceUri, String restrictAuthMethods, Duration tokenExpirationDuration) {
        this.accountCreation = accountCreation;
        this.accountCreationPasswordHandling = accountCreationPasswordHandling;
        this.expectedPasswordComplexity = expectedPasswordComplexity;
        this.expectedMinPasswordLength = expectedMinPasswordLength;
        this.expectedMaxPasswordLength = expectedMaxPasswordLength;
        this.usernamePolicy = usernamePolicy;
        this.emailConfirmationEnabled = emailConfirmationEnabled;
        this.adminEmailAddress = adminEmailAddress;
        this.fromEmailAddress = fromEmailAddress;
        this.createLdapAccountsEnabled = createLdapAccountsEnabled;
        this.accountCreationAuthWorkspaceUri = accountCreationAuthWorkspaceUri;
        this.restrictAuthMethods = restrictAuthMethods;
        this.tokenExpirationDuration = tokenExpirationDuration;
    }

}
