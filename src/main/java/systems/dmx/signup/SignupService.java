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

    InitiatePasswordResetRequestResult requestInitiatePasswordReset(String email, String displayName);

    PasswordResetRequestResult requestPasswordReset(String key);

    PasswordChangeRequestResult requestPasswordChange(String key, String password);
    
    Topic handleCustomAJAXSignupRequest(String username, String mailbox, String displayName, String password)
        throws URISyntaxException;

    /**
     *
     * @return  String  Workspace Topic ID
     */
    String createAPIWorkspaceMembershipRequest();

    SignUpRequestResult requestSignUp(String username, String mailbox, String displayName, String password, boolean skipConfirmation);

    ProcessSignUpRequestResult requestProcessSignUp(String key);

    /**
     * Sends out a valid password-reset token (if the email address is known to the system).
     * @throws URISyntaxException
     * @param email
     * @param redirectUrl
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
