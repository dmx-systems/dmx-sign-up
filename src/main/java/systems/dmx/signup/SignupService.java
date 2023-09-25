package systems.dmx.signup;


import java.net.URISyntaxException;
import java.util.List;

import systems.dmx.core.Topic;
import systems.dmx.signup.configuration.ModuleConfiguration;

/**
 * A plugin service to check username or mailbox availability and to send
 * out system or user mailbox notifications.
 * @version 1.5.1
 * @author Malte Rei&szlig;
 */

public interface SignupService {

    void setEmailTextProducer(EmailTextProducer emailTextProducer);

    String getSystemEmailContactOrEmpty();

    /**
     * Returns the display name of the given user.
     *
     * @return      the user's display name, or null if no display name is stored (or is not readable),
     *              or if the given username is unknown.
     *
     * @throws      RuntimeException    if an error occurs
     */
    String getDisplayName(String username);

    void updateDisplayName(String username, String displayName);

    /**
     * Sends a password-reset mail.
     * The mail will contain a link that includes the created Password Reset Token.
     *
     * TODO: rename to sendPasswordResetMail() (jri 2023/09/25)
     */
    InitiatePasswordResetRequestResult requestInitiatePasswordReset(String email, String displayName);

    /**
     * Checks if the Password Reset Token stored under "key" is valid.
     *
     * TODO: rename to checkToken() (jri 2023/09/25)
     *
     * @returns     a result whose "code" is one of SUCCESS, INVALID_TOKEN, LINK_EXPIRED, UNEXPECTED_ERROR
     */
    PasswordResetRequestResult requestPasswordReset(String key);

    /**
     * Changes the password of the user of the Password Reset Token stored under "key".
     *
     * TODO: rename to changePassword() (jri 2023/09/25)
     *
     * @param key           the key to access the password-reset token
     * @param password      the new password
     *
     * @return Returns the correct template for the input       FIXDOC
     */
    PasswordChangeRequestResult requestPasswordChange(String key, String password);
    
    /**
     * Creates a user account with display name, email address as username and password. FIXDOC
     *
     * TODO: rename to createUserAccount() (jri 2023/09/25)
     *
     * Requires the currently logged in user to be a member of the administration workspace or a member of a designated
     * workspace (specified through the configuration). FIXDOC
     *
     * @param mailbox       String must be unique
     * @param displayName   String
     * @param password      String For LDAP window.btoa encoded and for DMX -SHA-256- encoded
     *
     * @return
     */
    Topic handleCustomAJAXSignupRequest(String username, String mailbox, String displayName, String password)
        throws URISyntaxException;

    /**
     * @return  String  Workspace Topic ID
     */
    String createAPIWorkspaceMembershipRequest();

    SignUpRequestResult requestSignUp(String username, String mailbox, String displayName, String password,
                                      boolean skipConfirmation);

    ProcessSignUpRequestResult requestProcessSignUp(String key);

    /**
     * Sends out a valid password-reset token (if the email address is known to the system).
     *
     * @param email
     * @param redirectUrl
     *
     * @throws URISyntaxException
     *
     * @return Redirects the request to either "/sign-up/token-info" or "/sign-up/error", depending on the address.
     */
    InitiatePasswordResetRequestResult requestInitiateRedirectPasswordReset(String email, String redirectUrl);

    boolean isLdapAccountCreationEnabled();

    boolean isMailboxTaken(String value);

    boolean isUsernameTaken(String value);

    Boolean isLoggedIn();

    boolean isSelfRegistrationEnabled();

    boolean hasAccountCreationPrivilege();

    boolean isApiWorkspaceMember();

    List<String> getAuthorizationMethods();

    ModuleConfiguration getConfiguration();
}
