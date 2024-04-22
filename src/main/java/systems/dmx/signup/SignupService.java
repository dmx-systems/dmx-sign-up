package systems.dmx.signup;


import systems.dmx.core.Topic;
import systems.dmx.signup.configuration.Configuration;

import java.net.URISyntaxException;
import java.util.List;

/**
 * A plugin service to check username or mailbox availability and to send
 * out system or user mailbox notifications.
 * @version 1.5.1
 * @author Malte Rei&szlig;
 */

public interface SignupService {

    /**
     * Returns the configuration in use for this plugin instance.
     *
     * @return
     */
    Configuration getConfiguration();

    /**
     * Sets the {@link EmailTextProducer} instance that is called whenever email subjects and bodies need to be created.
     *
     * @param emailTextProducer New instance to set - must not be null
     */
    void setEmailTextProducer(EmailTextProducer emailTextProducer);

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
    InitiatePasswordResetRequestResult requestInitiatePasswordReset(String emailAddress, String displayName);

    /**
     * Checks if the given password reset token can be used.
     *
     * TODO: rename to checkToken() (jri 2023/09/25)
     *
     * @returns     a result whose "code" is one of SUCCESS, INVALID_TOKEN, LINK_EXPIRED, UNEXPECTED_ERROR
     */
    PasswordResetTokenCheckRequestResult requestPasswordResetTokenCheck(String token);

    /**
     * Changes the password of the user of the Password Reset Token stored under "key".
     *
     * TODO: rename to changePassword() (jri 2023/09/25)
     *
     * @param token     the token associated with a password change process
     * @param password  the new password for the user in plaintext
     *
     * @return A result whose "code" indicates the conclusion of the operation.
     */
    PasswordChangeRequestResult requestPasswordChange(String token, String password);
    
    /**
     * Creates a user account with display name, email address as username and password.
     *
     * Parts of the given arguments might be ignored depending on the configured username policy.
     *
     * Requires the currently logged in user to be a member of the administration workspace or a member of a designated
     * workspace (specified through the configuration).
     *
     * This function skips any kind of token handling and creates the user right away regardless of whether
     * email confirmation or self sign-up has been configured. As such this method is supposed to be employed only
     * for setting up user in an automated way. E.g. test users for a staging environment.
     *
     * @param username Username of the new user.
     * @param emailAddress Email address of the new user.
     * @param displayName Display name of the new user.
     * @param password Password of the new user.
     *
     * @return Username Topic of the newly created user or null.
     */
    Topic createUserAccount(String username, String emailAddress, String displayName, String password);

    /**
     * @return  String  Workspace Topic ID
     */

    SignUpRequestResult requestSignUp(String username, String emailAddress, String displayName, String password,
                                      boolean skipConfirmation);

    ProcessSignUpRequestResult requestProcessSignUp(String token);

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

    /** Returns whether a password can be set during account creation or not. */
    boolean isAccountCreationPasswordEditable();

    boolean isEmailAddressTaken(String value);

    boolean isUsernameTaken(String value);

    Boolean isPasswordComplexEnough(String password);

    Boolean isLoggedIn();

    boolean isSelfRegistrationEnabled();

    boolean hasAccountCreationPrivilege();

    List<String> getAuthorizationMethods();
}
