package systems.dmx.signup;

import com.sun.jersey.core.util.Base64;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.framework.BundleContext;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.core.Assoc;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.EventListener;
import systems.dmx.core.service.*;
import systems.dmx.core.service.accesscontrol.AccessControlException;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.core.service.event.PostUpdateTopic;
import systems.dmx.core.storage.spi.DMXTransaction;
import systems.dmx.facets.FacetsService;
import systems.dmx.ldap.service.LDAPPluginService;
import systems.dmx.sendmail.SendmailService;
import systems.dmx.signup.configuration.AccountCreation;
import systems.dmx.signup.configuration.ModuleConfiguration;
import systems.dmx.signup.configuration.SignUpConfigOptions;
import systems.dmx.signup.mapper.IsValidEmailAdressMapper;
import systems.dmx.signup.mapper.NewAccountDataMapper;
import systems.dmx.signup.model.NewAccountData;
import systems.dmx.signup.model.PasswordResetToken;
import systems.dmx.signup.model.NewAccountToken;
import systems.dmx.workspaces.WorkspacesService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static systems.dmx.accesscontrol.Constants.*;
import static systems.dmx.core.Constants.*;
import static systems.dmx.signup.Constants.*;
import static systems.dmx.signup.configuration.SignUpConfigOptions.*;

/**
 * This plugin enables anonymous users to create themselves a user account in DMX through an (optional) Email based
 * confirmation workflow and thus it depends on the dmx-sendmail plugin and e.g. postfix like "internet" installation
 * for "localhost". Source code available at: https://git.dmx.systems/dmx-plugins/dmx-sign-up
 *
 * @version 2.0.0-SNAPSHOT
 * @author Malte Rei&szlig;ig et al
**/
@Path("/sign-up")
@Produces(MediaType.APPLICATION_JSON)
public class SignupPlugin extends PluginActivator implements SignupService, PostUpdateTopic {

    private static final Logger log = Logger.getLogger(SignupPlugin.class.getName());

    private ModuleConfiguration activeModuleConfiguration = null;
    private Topic customWorkspaceAssignmentTopic = null;
    private String systemEmailContact = null;

    @Inject
    private AccessControlService accesscontrol;
    @Inject
    private FacetsService facets;
    @Inject
    private SendmailService sendmail;
    @Inject
    private WorkspacesService workspaces;

    private OptionalService<LDAPPluginService> ldapPluginService;

    @Context
    UriInfo uri;

    HashMap<String, NewAccountToken> newAccountTokens = new HashMap<>();
    HashMap<String, PasswordResetToken> passwordResetTokens = new HashMap<>();

    EmailTextProducer emailTextProducer = new InternalEmailTextProducer();

    private NewAccountDataMapper newAccountDataMapper = new NewAccountDataMapper();

    private IsValidEmailAdressMapper isValidEmailAdressMapper = new IsValidEmailAdressMapper();

    @Override
    public void init() {
        initOptionalServices();
        reloadAssociatedSignupConfiguration();
        // Log configuration settings
        log.info("\n  dmx.signup.account_creation: " + CONFIG_ACCOUNT_CREATION + "\n"
            + "  dmx.signup.account_creation_password_handling: " + CONFIG_ACCOUNT_CREATION_PASSWORD_HANDLING + "\n"
            + "  dmx.signup.username_policy: " + CONFIG_USERNAME_POLICY + "\n"
            + "  dmx.signup.confirm_email_address: " + CONFIG_EMAIL_CONFIRMATION + "\n"
            + "  dmx.signup.admin_mailbox: " + CONFIG_ADMIN_MAILBOX + "\n"
            + "  dmx.signup.system_mailbox: " + CONFIG_FROM_MAILBOX + "\n"
            + "  dmx.signup.ldap_account_creation: " + CONFIG_CREATE_LDAP_ACCOUNTS + "\n"
            + "  dmx.signup.account_creation_auth_ws_uri: " + CONFIG_ACCOUNT_CREATION_AUTH_WS_URI + "\n"
            + "  dmx.signup.restrict_auth_methods: " + CONFIG_RESTRICT_AUTH_METHODS + "\n"
            + "  dmx.signup.token_expiration_time: " + CONFIG_TOKEN_EXPIRATION_DURATION.toHours() + "\n"
        );
        log.info("Available auth methods and order:" + getAuthorizationMethods() + "\n");
        if (CONFIG_CREATE_LDAP_ACCOUNTS && !isLdapPluginAvailable()) {
            log.warning("LDAP Account creation configured but respective plugin not available!");
        }
    }

    @Override
    public void setEmailTextProducer(EmailTextProducer emailTextProducer) {
        if (emailTextProducer == null) {
            throw new IllegalArgumentException("New instance cannot be null");
        }
        this.emailTextProducer = emailTextProducer;
    }

    @Override
    public String getSystemEmailContactOrEmpty() {
        return (systemEmailContact == null) ? "" : systemEmailContact;
    }

    private void initOptionalServices() {
        ldapPluginService = new OptionalService<>(getBundleContext(), () -> LDAPPluginService.class);
    }

    @Override
    public void stop(BundleContext context) {
        ldapPluginService.release();
        super.stop(context);
    }

    /**
     * Custom event fired by sign-up module up on successful user account creation.
     *
     * @return Topic	The username topic (related to newly created user account
     * topic).
     */
    static DMXEvent USER_ACCOUNT_CREATE_LISTENER = new DMXEvent(UserAccountCreateListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((UserAccountCreateListener) listener).userAccountCreated((Topic) params[0]);
        }
    };

    // --- Plugin Service Implementation --- //
    /**
     * A HTTP resource allowing existence checks for given username strings.
     * @param username
     * @return A String being a JSONObject with an "isAvailable" property being either "true" or "false".
     *
     * If the username is already taken isAvailable is set to false.
     */
    @GET
    @Path("/check/{username}")
    public String getUsernameAvailability(@PathParam("username") String username) {
        JSONObject response = new JSONObject();
        try {
            response.put("isAvailable", true);
            if (isUsernameTaken(username)) {
                response.put("isAvailable", false);
            }
            return response.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/display-name/{username}")
    @Override
    public String getDisplayName(@PathParam("username") String username) {
        try {
            Topic usernameTopic = accesscontrol.getUsernameTopic(username);
            if (usernameTopic != null) {
                Topic displayName = facets.getFacet(usernameTopic, DISPLAY_NAME_FACET);
                if (displayName != null) {
                    return displayName.getSimpleValue().toString();
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Fetching display name of user \"" + username + "\" failed", e);
        }
    }

    @PUT
    @Path("/display-name/{username}")
    @Transactional
    @Override
    public void updateDisplayName(@PathParam("username") String username,
                                  @QueryParam("displayName") String displayName) {
        try {
            long workspaceId = getDisplayNamesWorkspaceId();
            dmx.getPrivilegedAccess().runInWorkspaceContext(workspaceId, () -> {
                Topic usernameTopic = accesscontrol.getUsernameTopic(username);
                if (usernameTopic != null) {
                    facets.updateFacet(usernameTopic, DISPLAY_NAME_FACET,
                        mf.newFacetValueModel(DISPLAY_NAME).set(displayName)
                    );
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Updating display name of user \"" + username + "\" failed, displayName=\"" +
                displayName + "\"", e);
        }
    }

    /**
     * A HTTP resource allowing existence check fors the given email address string.
     * @param email
     * @return A String being a JSONObject with an "isAvailable" property being either "true" or "false".
     * If the email address is already known in the system isAvailable is set to false.
     */
    @GET
    @Path("/check/mailbox/{email}")
    public String getMailboxAvailability(@PathParam("email") String email) {
        JSONObject response = new JSONObject();
        try {
            response.put("isAvailable", true);
            if (isMailboxTaken(email)) {
                response.put("isAvailable", false);
            }
            return response.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private NewAccountData mapToNewAccountData(String username, String mailbox, String displayName) {
        return newAccountDataMapper.map(CONFIG_USERNAME_POLICY, username, mailbox, displayName);
    }

    @Override
    public SignUpRequestResult requestSignUp(String username, String mailbox, String displayName, String password,
                                             boolean skipConfirmation) {
        if (!isSelfRegistrationEnabled() && !hasAccountCreationPrivilege()) {
            return new SignUpRequestResult(SignUpRequestResult.Code.ACCOUNT_CREATION_DENIED);
        }
        if (!isValidEmailAdressMapper.map(mailbox)) {
            return new SignUpRequestResult(SignUpRequestResult.Code.ERROR_INVALID_EMAIL);
        }
        NewAccountData newAccountData = mapToNewAccountData(username, mailbox, displayName);
        try {
            if (SignUpConfigOptions.CONFIG_EMAIL_CONFIRMATION) {
                return handleSignUpWithEmailConfirmation(newAccountData, password, skipConfirmation);
            } else {
                return handleSignUpWithDirectAccountCreation(newAccountData, password);
            }
        } catch (URISyntaxException e) {
            log.log(Level.SEVERE, "Could not build response URI while handling sign-up request", e);
        }
        return new SignUpRequestResult(SignUpRequestResult.Code.UNEXPECTED_ERROR);
    }
    private SignUpRequestResult handleSignUpWithDirectAccountCreation(
            NewAccountData newAccountData,
            String password) throws URISyntaxException {
        // ensures that logged in user is an admin or account creation is allowed for everyone
        if (isSelfRegistrationEnabled() || hasAccountCreationPrivilege()) {
            try {
                transactional(() -> createCustomUserAccount(newAccountData, password));
            } catch (Exception e) {
                return new SignUpRequestResult(SignUpRequestResult.Code.UNEXPECTED_ERROR);
            }
            return handleAccountCreatedRedirect(newAccountData.username);
        } else {
            return new SignUpRequestResult(SignUpRequestResult.Code.ACCOUNT_CREATION_DENIED);
        }
    }

    private SignUpRequestResult handleSignUpWithEmailConfirmation(
            NewAccountData newAccountData,
            String password,
            boolean skipConfirmation) {
        if (skipConfirmation && hasAccountCreationPrivilege()) {
            if (SignUpConfigOptions.CONFIG_ACCOUNT_CREATION == AccountCreation.ADMIN) {
                log.info("Sign-up Configuration: Email based confirmation workflow active, Administrator " +
                    "skipping confirmation mail.");
                try {
                    transactional(() -> createCustomUserAccount(newAccountData, password));
                } catch (Exception e) {
                    return new SignUpRequestResult(SignUpRequestResult.Code.UNEXPECTED_ERROR);
                }
                return handleAccountCreatedRedirect(newAccountData.username);
            } else {
                // skipping confirmation is only allowed for admins
                return new SignUpRequestResult(SignUpRequestResult.Code.ADMIN_PRIVILEGE_MISSING);
            }
        } else {
            log.info("Sign-up Configuration: Email based confirmation workflow active, send out " +
                "confirmation mail.");
            String tokenKey = createUserValidationToken(newAccountData, password);
            sendConfirmationMail(tokenKey, newAccountData.displayName, newAccountData.email);
            // redirect user to a "token-info" page
            return new SignUpRequestResult(SignUpRequestResult.Code.SUCCESS_EMAIL_CONFIRMATION_NEEDED);
        }
    }

    private SignUpRequestResult handleAccountCreatedRedirect(String username) {
        if (SignUpConfigOptions.DMX_ACCOUNTS_ENABLED) {
            log.info("DMX Config: The new account is now ENABLED, redirecting to OK page.");
            // redirecting user to the "your account is now active" page
            return new SignUpRequestResult(SignUpRequestResult.Code.SUCCESS_ACCOUNT_CREATED, username);
        } else {
            log.info("DMX Config: The new account is now DISABLED, redirecting to PENDING page.");
            // redirecting to page displaying "your account was created but needs to be activated"
            return new SignUpRequestResult(SignUpRequestResult.Code.SUCCESS_ACCOUNT_PENDING, username);
        }
    }

    @Override
    public ProcessSignUpRequestResult requestProcessSignUp(String key) {
        // 1) Assert token exists: It may not exist due to e.g. bundle refresh, system restart, token invalid
        if (!newAccountTokens.containsKey(key)) {
            return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.INVALID_TOKEN);
        }
        // 2) Process available token and remove it from stack
        NewAccountToken token = newAccountTokens.remove(key);
        // 3) Create the user account and show ok OR present an error message.
        try {
            if (token.expiration.isAfter(Instant.now())) {
                log.log(Level.INFO, "Trying to create user account for {0}", token.accountData.email);
                try {
                    transactional(() -> createCustomUserAccount(token.accountData, token.password));
                } catch (Exception e) {
                    return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.UNEXPECTED_ERROR);
                }
            } else {
                return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.LINK_EXPIRED);
            }
        } catch (RuntimeException ex) {
            log.log(Level.SEVERE, null, ex);
            log.log(Level.SEVERE, "Account creation failed due to {0} caused by {1}",
                new Object[]{ex.getMessage(), ex.getCause().toString()});
            return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.UNEXPECTED_ERROR);
        }
        log.log(Level.INFO, "Account succesfully created for username: {0}", token.accountData.username);
        if (!SignUpConfigOptions.DMX_ACCOUNTS_ENABLED) {
            log.log(Level.INFO, "> Account activation by an administrator remains PENDING ");
            return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.SUCCESS_ACCOUNT_PENDING);
        }
        return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.SUCCESS, token.accountData.username);
    }

    @Override
    public InitiatePasswordResetRequestResult requestInitiateRedirectPasswordReset(String email, String redirectUrl) {
        log.info("Password reset requested for user with Email: \"" + email + "\" wishing to redirect to: \"" +
            redirectUrl + "\"");
        String emailAddressValue = email.trim();
        boolean emailExists = dmx.getPrivilegedAccess().emailAddressExists(emailAddressValue);
        if (emailExists) {
            log.info("Email based password reset workflow do'able, sending out passwort reset mail.");
            // ### Todo: Add/include return Url to token (!)
            // Note: Here system can't know "display name" (anonymous has
            // no read permission on it) and thus can't pass it on
            // TODO: use privileged API to get display name
            sendPasswordResetToken(emailAddressValue, null, redirectUrl);
            return InitiatePasswordResetRequestResult.SUCCESS;
        }
        log.warning("Email based password reset workflow not do'able, Email Address does NOT EXIST => " + email.trim());
        return InitiatePasswordResetRequestResult.EMAIL_UNKNOWN;
    }

    // Note: called by anonymous (a user forgot his password), so it must be @GET.
    // For anonymous @POST/@PUT would be rejected by DMX platform's request filter.
    @GET
    @Path("/password-reset/{email}")
    @Override
    public InitiatePasswordResetRequestResult requestInitiatePasswordReset(@PathParam("email") String email,
                                                                           @QueryParam("name") String displayName) {
        log.info("Password reset requested for user with Email: \"" + email + "\" and Name: \"" + displayName + "\"");
        try {
            String emailAddressValue = email.trim();
            if (!isValidEmailAdressMapper.map(emailAddressValue)) {
                return InitiatePasswordResetRequestResult.UNEXPECTED_ERROR;
            }
            boolean emailExists = dmx.getPrivilegedAccess().emailAddressExists(emailAddressValue);
            if (emailExists) {
                log.info("Email based password reset workflow do'able, sending out passwort reset mail.");
                sendPasswordResetToken(emailAddressValue, displayName, null);
                return InitiatePasswordResetRequestResult.SUCCESS;
            } else {
                log.info("Email based password reset workflow not do'able, Email Address does NOT EXIST => " +
                    emailAddressValue.trim());
                return InitiatePasswordResetRequestResult.EMAIL_UNKNOWN;
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
        return InitiatePasswordResetRequestResult.UNEXPECTED_ERROR;
    }

    @GET
    @Path("/token/{key}")
    @Override
    public PasswordResetRequestResult requestPasswordReset(@PathParam("key") String key) {
        // 1) Assert token exists: It may not exist due to e.g. bundle refresh, system restart, token invalid
        if (!passwordResetTokens.containsKey(key)) {
            return new PasswordResetRequestResult(PasswordResetRequestResult.Code.INVALID_TOKEN);
        }
        // 2) Process available token and remove it from stack
        PasswordResetToken token = passwordResetTokens.get(key);
        // 3) Update the user account credentials OR present an error message.
        if (token != null && token.expiration.isAfter(Instant.now())) {
            return new PasswordResetRequestResult(PasswordResetRequestResult.Code.SUCCESS, token.accountData.username,
                token.accountData.email, token.accountData.displayName, token.redirectUrl);
        } else {
            log.warning("The link to reset the password for " + token.accountData.username + " has expired.");
            passwordResetTokens.remove(key);
            return new PasswordResetRequestResult(PasswordResetRequestResult.Code.LINK_EXPIRED);
        }
    }

    // TODO: drop this HTTP-facade. Instead make isSelfRegistrationEnabled() RESTful in a JAX-RS fashion.
    // Possibly extend platform's dmx-webservice module with a provider class for "boolean" type (jri 2023/09/28)
    @GET
    @Path("/self-registration-active")
    public Response getSelfRegistrationStatus() {
        return Response.ok("" + isSelfRegistrationEnabled()).build();
    }

    // Note: called by anonymous (a user forgot his password), so it must be @GET.
    // For anonymous @POST/@PUT would be rejected by DMX platform's request filter.
    @GET
    @Path("/password-reset/{key}/{password}")
    @Transactional
    @Override
    public PasswordChangeRequestResult requestPasswordChange(@PathParam("key") String key,
                                                             @PathParam("password") String password) {
        log.info("Processing Password Update Request Token... ");
        PasswordResetToken token = passwordResetTokens.get(key);
        if (token != null) {
            Credentials newCreds = new Credentials("dummy", "pass");
            newCreds.username = token.accountData.username;
            if (!isLdapAccountCreationEnabled()) {
                newCreds.password = password;
                // Change password stored in "User Account" topic
                dmx.getPrivilegedAccess().changePassword(newCreds);
                log.info("Credentials for user " + newCreds.username + " were changed succesfully.");
            } else {
                String plaintextPassword = Base64.base64Decode(password);
                log.info("Change password attempt for \"" + newCreds.username + "\". password-value string " +
                    "provided by client \"" + password + "\", plaintextPassword: \"" + plaintextPassword + "\"");
                // The tendu-way (but with base64Decode, as sign-up frontend encodes password using window.btoa)
                newCreds.plaintextPassword = plaintextPassword;
                newCreds.password = password; // should not be in effect since latest dmx-ldap SNAPSHOT
                if (ldapPluginService.get().changePassword(newCreds) != null) {
                    log.info("If no previous errors are reported here or in the LDAP-service log, the " +
                        "credentials for user " + newCreds.username + " should now have been changed succesfully.");
                } else {
                    log.severe("Credentials for user " + newCreds.username + " COULD NOT be changed succesfully.");
                    return PasswordChangeRequestResult.PASSWORD_CHANGE_FAILED;
                }
            }
            passwordResetTokens.remove(key);
            return PasswordChangeRequestResult.SUCCESS;
        } else {
            return PasswordChangeRequestResult.NO_TOKEN;
        }
    }

    @POST
    @Path("/user-account/{username}/{mailbox}/{displayname}/{password}")
    @Transactional
    @Override
    public Topic createUserAccount(@PathParam("username") String username,
                                   @PathParam("mailbox") String mailbox,
                                   @PathParam("displayname") String displayName,
                                   @PathParam("password") String password) {
        log.info("Creating user account with display name \"" + displayName + "\" and email address \"" + mailbox +
            "\"");
        checkAccountCreation();
        Topic usernameTopic = createCustomUserAccount(mapToNewAccountData(username, mailbox, displayName), password);
        return usernameTopic;
    }

    private Topic createCustomUserAccount(NewAccountData newAccountData, String password) {
        try {
            // 1) NewAccountData is set according to username policy
            String username = createSimpleUserAccount(newAccountData.username.trim(), password,
                newAccountData.email.trim());
            // 2) create and assign displayname topic to "System" workspace
            final String displayNameValue = newAccountData.displayName.trim();
            final Topic usernameTopic = accesscontrol.getUsernameTopic(username);
            final long usernameTopicId = usernameTopic.getId();
            long displayNamesWorkspaceId = getDisplayNamesWorkspaceId();
            dmx.getPrivilegedAccess().runInWorkspaceContext(-1, new Callable<Topic>() {
                @Override
                public Topic call() {
                    // create display name facet for username topic
                    facets.addFacetTypeToTopic(usernameTopicId, DISPLAY_NAME_FACET);
                    // TODO: Not doable for anonymous user. Needs a privileged function.
                    facets.updateFacet(usernameTopicId, DISPLAY_NAME_FACET, mf.newFacetValueModel(DISPLAY_NAME)
                        .set(displayNameValue));
                    // automatically make users member in "Display Names" workspace
                    dmx.getPrivilegedAccess().createMembership(username, displayNamesWorkspaceId);
                    log.info("Created membership for new user account in \"Display Names\" workspace " +
                        "(SharingMode.Collaborative)");
                    // Account creator should be member of "Display Names" ..
                    // or is "runInWorkspacecContext privileged to GET?
                    RelatedTopic result = facets.getFacet(usernameTopicId, DISPLAY_NAME_FACET);
                    dmx.getPrivilegedAccess().assignToWorkspace(result, displayNamesWorkspaceId);
                    return result;
                }
            });
            return usernameTopic;
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to create custom account", e);
            throw new RuntimeException("Creating custom user account failed, mailbox=\"" + newAccountData.email +
                "\", displayName=\"" + newAccountData.displayName + "\"", e);
        }
    }

    public long getDisplayNamesWorkspaceId() {
        Topic ws = workspaces.getWorkspace(DISPLAY_NAME_WS_URI);
        return (ws != null) ? ws.getId() : -1;
    }

    /**
     * A HTTP resource to associate the requesting username with
     * the "Custom Membership Request" note topic and to inform the administrators by email.
     * @return String containing a JSONObject with an "membership_created" r´property representing the relation.
     */
    @POST
    @Path("/confirm/membership/custom")
    @Transactional
    @Override
    public String createAPIWorkspaceMembershipRequest() {
        Topic apiMembershipRequestNote = dmx.getTopicByUri("dmx.signup.api_membership_requests");
        if (apiMembershipRequestNote != null && accesscontrol.getUsername() != null) {
            Topic usernameTopic = accesscontrol.getUsernameTopic();
            // 1) Try to manage workspace membership directly (success depends on ACL and the SharingMode of the
            // configured workspace)
            createApiWorkspaceMembership(usernameTopic); // might fail silently
            // 2) Store API Membership Request in a Note (residing in the "System" workspace) association
            Assoc requestRelation = getDefaultAssociation(usernameTopic.getId(), apiMembershipRequestNote.getId());
            if (requestRelation == null) {
                // ### Fixme: For the moment it depends on (your web application, more specifically) the workspace
                // cookie set (at the requesting client) which workspace this assoc will be assigned to
                createApiMembershipRequestNoteAssociation(usernameTopic, apiMembershipRequestNote);
            } else {
                String username = usernameTopic.getSimpleValue().toString();
                log.info("Revoke Request for API Workspace Membership by user \"" + username + "\"");
                String api_usage_revoked = emailTextProducer.getApiUsageRevokedMailSubject();
                String message = emailTextProducer.getApiUsageRevokedMailText(username);
                sendSystemMailboxNotification(api_usage_revoked, message);
                // 2.1) fails in all cases where user has no write access to the workspace the association was created
                // in dmx.deleteAssociation(requestRelation.getId());
                // For now: API Usage Membership must be revoked per Email but personally and confirmed by the
                // administrator. A respective hint was place in the "API Usage" dialog on the users account
                // (/sign-up/edit) page.
            }
            return "{ \"membership_created\" : " + true + "}";
        } else {
            return "{ \"membership_created\" : " + false + "}";
        }
    }

    @Override
    public void postUpdateTopic(Topic topic, ChangeReport report, TopicModel updateModel)  {
        if (topic.getTypeUri().equals(SIGN_UP_CONFIG_TYPE_URI)) {
            reloadAssociatedSignupConfiguration();
        } else if (topic.getTypeUri().equals(LOGIN_ENABLED)) {
            // Account status
            boolean status = Boolean.parseBoolean(topic.getSimpleValue().toString());
            // Account involved
            Topic username = topic.getRelatedTopic("dmx.config.configuration", null, null, USERNAME);
            // Perform notification
            if (status && !DMX_ACCOUNTS_ENABLED) { // Enabled=true && new_accounts_are_enabled=false
                log.info("Sign-up Notification: User Account \"" + username.getSimpleValue() + "\" is now ENABLED!");
                Topic mailbox = username.getRelatedTopic(USER_MAILBOX_EDGE_TYPE, null, null, USER_MAILBOX_TYPE_URI);
                if (mailbox != null) { // for accounts created via sign-up plugin this will always evaluate to true
                    String mailboxValue = mailbox.getSimpleValue().toString();
                    String subject = emailTextProducer.getAccountActiveEmailSubject();
                    String message = emailTextProducer.getAccountActiveEmailMessage(username.toString());
                    sendMail(subject, message, mailboxValue);
                    log.info("Send system notification mail to " + mailboxValue + " - The account is now active!");
                }
            }
        }
    }

    // --- Sign-up Plugin Routes --- //

    @Override
    public Boolean isLoggedIn() {
        return accesscontrol.getUsername() != null;
    }

    private void sendSystemMailboxNotification(String subject, String message) {
        if (!CONFIG_ADMIN_MAILBOX.isEmpty()) {
            String recipient = CONFIG_ADMIN_MAILBOX;
            try {
                sendMail(subject, message, recipient);
            } catch (Exception ex) {
                log.severe("There seems to be an issue with your mail (SMTP) setup, we FAILED sending out a " +
                    "notification mail to the \"System Mailbox\", caused by: " + ex.getMessage());
            }
        } else {
            log.info("Did not send notification mail to System Mailbox - Admin Mailbox Empty");
        }
    }

    private void sendUserMailboxNotification(String mailbox, String subject, String message) {
        try {
            sendMail(subject, message, mailbox);
        } catch (Exception ex) {
            log.severe("There seems to be an issue with your mail (SMTP) setup, we FAILED sending out a " +
                "notification mail to User \"" + mailbox + "\", caused by: " + ex.getMessage());
        }
    }

    private boolean isLdapPluginAvailable() {
        try {
            return ldapPluginService.get() != null;
        } catch (NoClassDefFoundError error) {
            return false;
        }
    }

    @Override
    public boolean isLdapAccountCreationEnabled() {
        return CONFIG_CREATE_LDAP_ACCOUNTS && isLdapPluginAvailable();
    }

    private boolean isAccountCreationPasswordEditable() {
        return CONFIG_ACCOUNT_CREATION_PASSWORD_HANDLING == AccountCreation.PasswordHandling.EDITABLE;
    }

    private Topic createUsername(Credentials credentials) throws Exception {
        if (isLdapAccountCreationEnabled()) {
            return ldapPluginService.get().createUser(credentials);
        } else {
            return accesscontrol._createUserAccount(credentials);
        }
    }

    private String createSimpleUserAccount(String username, String password, String mailbox) {
        try {
            if (isUsernameTaken(username)) {
                // Might be thrown if two users compete for registration (of the same username)
                // within the same 60 minutes (tokens validity timespan). First confirming, wins.
                throw new RuntimeException("Username \"" + username + "\" was already registered and confirmed");
            }
            Credentials creds;
            // When the "Basic" method is used the password is already in -SHA256- form for all other
            // methods it is simply base64-encoded
            if (!isLdapAccountCreationEnabled()) {
                creds = new Credentials(new JSONObject()
                    .put("username", username.trim())
                    .put("password", password.trim()));
            } else {
                String plaintextPassword = Base64.base64Decode(password);
                creds = new Credentials(username.trim(), plaintextPassword);
                // Retroactively provides plaintext password in credentials
                creds.plaintextPassword = plaintextPassword;
            }
            // 1) Creates a new username topic (in LDAP and/or DMX)
            final Topic usernameTopic = createUsername(creds);
            final String eMailAddressValue = mailbox;
            dmx.getPrivilegedAccess().runInWorkspaceContext(-1, new Callable<Topic>() {
                @Override
                public Topic call() {
                    // 2) create and associate e-mail address topic in "System" Workspace
                    long systemWorkspaceId = dmx.getPrivilegedAccess().getSystemWorkspaceId();
                    Topic eMailAddress = dmx.createTopic(mf.newTopicModel(USER_MAILBOX_TYPE_URI,
                        new SimpleValue(eMailAddressValue)));
                    dmx.getPrivilegedAccess().assignToWorkspace(eMailAddress, systemWorkspaceId);
                    // 3) fire custom event ### this is useless since fired by "anonymous" (this request scope)
                    dmx.fireEvent(USER_ACCOUNT_CREATE_LISTENER, usernameTopic);
                    // 4) associate email address to "username" topic too
                    Assoc assoc = dmx.createAssoc(mf.newAssocModel(USER_MAILBOX_EDGE_TYPE,
                        mf.newTopicPlayerModel(eMailAddress.getId(), CHILD),
                        mf.newTopicPlayerModel(usernameTopic.getId(), PARENT)));
                    dmx.getPrivilegedAccess().assignToWorkspace(assoc, systemWorkspaceId);
                    // 5) create membership to custom workspace topic
                    if (customWorkspaceAssignmentTopic != null) {
                        accesscontrol.createMembership(usernameTopic.getSimpleValue().toString(),
                            customWorkspaceAssignmentTopic.getId());
                        log.info("Created new Membership for " + usernameTopic.getSimpleValue().toString() + " in " +
                            "workspace=" + customWorkspaceAssignmentTopic.getSimpleValue().toString());
                    }
                    return eMailAddress;
                }
            });
            log.info("Created new user account for user \"" + username + "\" and " + eMailAddressValue);
            // 6) Inform administrations about successfull account creation
            sendNotificationMail(username, mailbox.trim());
            return username;
        } catch (Exception e) {
            log.log(Level.WARNING, "Creating simple user account failed", e);
            throw new RuntimeException("Creating simple user account failed, username=\"" + username +
                "\", mailbox=\"" + mailbox + "\"", e);
        }
    }

    @Override
    public boolean isMailboxTaken(String email) {
        String value = email.toLowerCase().trim();
        return dmx.getPrivilegedAccess().emailAddressExists(value);
    }

    @Override
    public boolean isUsernameTaken(String username) {
        String value = username.trim();
        Topic userNameTopic = accesscontrol.getUsernameTopic(value);
        return (userNameTopic != null);
    }

    // --- Private Helpers --- //

    @Override
    public boolean isSelfRegistrationEnabled() {
        return CONFIG_ACCOUNT_CREATION == AccountCreation.PUBLIC;
    }

    @Override
    public boolean hasAccountCreationPrivilege() {
        try {
            checkAccountCreation();
            return true;
        } catch (AccessControlException ace) {
            return false;
        } catch (RuntimeException re) {
            // Deals with unexpected behavior of DMX: On missing read permission RuntimeException is thrown
            return false;
        }
    }

    private void checkAccountCreation() {
        if (isAccountCreationWorkspaceUriConfigured()) {
            try {
                checkAccountCreationWorkspaceWriteAccess();
            } catch (AccessControlException ace) {
                checkAdministrationWorkspaceWriteAccess();
            } catch (RuntimeException re) {
                // Deals with unexpected behavior of DMX: On missing read permission RuntimeException is thrown
                checkAdministrationWorkspaceWriteAccess();
            }
        } else {
            checkAdministrationWorkspaceWriteAccess();
        }
    }
    
    private void checkAdministrationWorkspaceWriteAccess() {
        dmx.getTopic(dmx.getPrivilegedAccess().getAdminWorkspaceId()).checkWriteAccess();
    }

    private boolean isAccountCreationWorkspaceUriConfigured() {
        return !CONFIG_ACCOUNT_CREATION_AUTH_WS_URI.isEmpty();
    }

    private void checkAccountCreationWorkspaceWriteAccess() {
        dmx.getTopic(workspaces.getWorkspace(CONFIG_ACCOUNT_CREATION_AUTH_WS_URI).getId()).checkWriteAccess();
    }

    @Override
    public boolean isApiWorkspaceMember() {
        String username = accesscontrol.getUsername();
        if (username != null) {
            String apiWorkspaceUri = activeModuleConfiguration.getApiWorkspaceUri();
            if (!apiWorkspaceUri.isEmpty() && !apiWorkspaceUri.equals("undefined")) {
                Topic apiWorkspace = dmx.getPrivilegedAccess().getWorkspace(apiWorkspaceUri);
                if (apiWorkspace != null) {
                    return accesscontrol.isMember(username, apiWorkspace.getId());
                }
            } else {
                Topic usernameTopic = accesscontrol.getUsernameTopic();
                Topic apiMembershipRequestNote = dmx.getTopicByUri("dmx.signup.api_membership_requests");
                Assoc requestRelation = getDefaultAssociation(usernameTopic.getId(), apiMembershipRequestNote.getId());
                if (requestRelation != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendPasswordResetToken(String mailbox, String displayName, String redirectUrl) {
        String username = dmx.getPrivilegedAccess().getUsername(mailbox);
        // Todo: Would need privileged access to "Display Name" to display it in password-update dialog
        String tokenKey = createPasswordResetToken(username, mailbox, displayName, redirectUrl);
        sendPasswordResetMail(tokenKey, username, mailbox.trim(), displayName);
    }

    private String createUserValidationToken(NewAccountData newAccountData, String password) {
        String tokenKey = UUID.randomUUID().toString();
        Instant expiration = calculateTokenExpiration();
        NewAccountToken token = new NewAccountToken(newAccountData, password, expiration);
        newAccountTokens.put(tokenKey, token);
        log.log(Level.INFO, "Set up key {0} for {1} sending confirmation mail valid till {3}",
            new Object[]{ tokenKey, newAccountData.email, expiration });
        return tokenKey;
    }

    private Instant calculateTokenExpiration() {
        return Instant.now().plus(CONFIG_TOKEN_EXPIRATION_DURATION);
    }

    private String createPasswordResetToken(String username, String mailbox, String name, String redirectUrl) {
        String tokenKey = UUID.randomUUID().toString();
        Instant expiration = calculateTokenExpiration();
        PasswordResetToken token = new PasswordResetToken(
            new NewAccountData(username, mailbox, name),
            expiration,
            redirectUrl
        );
        passwordResetTokens.put(tokenKey, token);
        log.log(Level.INFO, "Set up pwToken {0} for {1} send passwort reset mail valid till {3}",
            new Object[]{tokenKey, mailbox, expiration});
        return tokenKey;
    }

    private void createApiMembershipRequestNoteAssociation(Topic usernameTopic, Topic membershipNote) {
        Assoc apiRequest = dmx.createAssoc(mf.newAssocModel(ASSOCIATION,
            mf.newTopicPlayerModel(usernameTopic.getId(), DEFAULT),
            mf.newTopicPlayerModel(membershipNote.getId(), DEFAULT)));
        dmx.getPrivilegedAccess().assignToWorkspace(apiRequest, dmx.getPrivilegedAccess().getSystemWorkspaceId());
        log.info("Request for new custom API Workspace Membership by user \"" +
            usernameTopic.getSimpleValue().toString() + "\"");
        String subject = emailTextProducer.getApiUsageRequestedSubject();
        String message = emailTextProducer.getApiUsageRequestedMessage(usernameTopic.getSimpleValue().toString());
        sendSystemMailboxNotification(subject, message);
    }

    private void createApiWorkspaceMembership(Topic usernameTopic) {
        String apiWorkspaceUri = activeModuleConfiguration.getApiWorkspaceUri();
        if (!apiWorkspaceUri.isEmpty() && !apiWorkspaceUri.equals("undefined")) { // don't use this option in production
            Topic apiWorkspace = dmx.getPrivilegedAccess().getWorkspace(apiWorkspaceUri);
            if (apiWorkspace != null) {
                log.info("Request for new custom API Workspace Membership by user \"" +
                    usernameTopic.getSimpleValue().toString() + "\"");
                // Attempt to create a Workspace membership for this Assocation/Relation
                accesscontrol.createMembership(usernameTopic.getSimpleValue().toString(), apiWorkspace.getId());
            } else {
                log.info("Revoke Request for API Workspace Membership by user \"" +
                    usernameTopic.getSimpleValue().toString() + "\"");
                if (accesscontrol.isMember(usernameTopic.getSimpleValue().toString(), apiWorkspace.getId())) {
                    Assoc assoc = getMembershipAssociation(usernameTopic.getId(), apiWorkspace.getId());
                    dmx.deleteAssoc(assoc.getId());
                } else {
                    log.info("Skipped Revoke Request for non-existent API Workspace Membership for \"" +
                        usernameTopic.getSimpleValue().toString() + "\"");
                }
            }
        } else {
            log.info("No API Workspace Configured: You must enter the URI of a programmatically created workspace " +
                "topic into your current \"Signup Configuration\".");
        }
    }

    /**
     * Loads the sign-up configuration, a topic of type "Sign-up Configuration" associated to this plugins
     * topic of type "Plugin".
     *
     * @see #init()
     * @see #postUpdateTopic(Topic, ChangeReport, TopicModel)
     */
    private void reloadAssociatedSignupConfiguration() {
        // load module configuration
        activeModuleConfiguration = loadConfiguration();
        if (!activeModuleConfiguration.isValid()) {
            log.warning("Could not load associated Sign-up Plugin Configuration Topic during init/postUpdate");
            return;
        }
        activeModuleConfiguration.reload();
        // check for custom workspace assignment
        customWorkspaceAssignmentTopic = activeModuleConfiguration.getCustomWorkspaceAssignmentTopic();
        if (customWorkspaceAssignmentTopic != null) {
            log.info("Configured Custom Sign-up Workspace => \"" + customWorkspaceAssignmentTopic.getSimpleValue() +
                "\"");
        }
        log.log(Level.INFO, "Sign-up Configuration Loaded (URI=\"{0}\"), Name=\"{1}\"", new Object[]{
            activeModuleConfiguration.getConfigurationUri(),
            activeModuleConfiguration.getConfigurationName()
        });
    }

    private void sendConfirmationMail(String key, String username, String mailbox) {
        try {
            if (DMX_ACCOUNTS_ENABLED) {
                String mailSubject = emailTextProducer.getConfirmationActiveMailSubject();
                String message = emailTextProducer.getConfirmationActiveMailMessage(username, key);
                sendMail(mailSubject, message, mailbox);
            } else {
                String mailSubject = emailTextProducer.getConfirmationProceedMailSubject();
                String message = emailTextProducer.getUserConfirmationProceedMailMessage(username, key);
                sendMail(mailSubject, message, mailbox);
            }
        } catch (RuntimeException ex) {
            log.severe("There seems to be an issue with your mail (SMTP) setup, we FAILED sending out the " +
                    "\"Email Confirmation\" mail, caused by: " + ex.getMessage());
            throw ex;
        }
    }

    private void sendPasswordResetMail(String key, String username, String mailbox, String displayName) {
        try {
            String addressee = (displayName != null && !displayName.isEmpty()) ? displayName : username;
            String subject = emailTextProducer.getPasswordResetMailSubject();
            String message = emailTextProducer.getPasswordResetMailMessage(addressee, key);
            sendMail(subject, message, mailbox);
        } catch (RuntimeException ex) {
            log.severe("There seems to be an issue with your mail (SMTP) setup, we FAILED sending out the " +
                "\"Password Reset\" mail, caused by: " + ex.getMessage());
            throw ex;
        }
    }

    /**
     * Sends an email to the admin (as configured by "dmx.signup.admin_mailbox") about successful account creation.
     */
    private void sendNotificationMail(String username, String mailbox) {
        if (CONFIG_ADMIN_MAILBOX != null && !CONFIG_ADMIN_MAILBOX.isEmpty()) {
            try {
                String subject = emailTextProducer.getAccountCreationSystemEmailSubject();
                String message = emailTextProducer.getAccountCreationSystemEmailMessage(username, mailbox);
                sendMail(subject, message, CONFIG_ADMIN_MAILBOX);
            } catch (Exception ex) {
                log.severe("There seems to be an issue with your mail (SMTP) setup, we FAILED notifying the " +
                    "\"system mailbox\" about account creation, caused by: " + ex.getMessage());
            }
        } else {
            log.warning("\"dmx.signup.admin_mailbox\" is not configured; mail about account creation (\"" + username +
                "\") could not be sent");
        }
    }

    /**
     *
     * @param subject           String Subject text for the message.
     * @param message           String Text content of the message.
     * @param recipientValues   String of Email Address message is sent to **must not** be NULL.
     */
    private void sendMail(String subject, String message, String recipientValues) {
        String projectName = "TODO"; // TODO?
        String sender = CONFIG_FROM_MAILBOX;
        boolean isHtml = emailTextProducer.isHtml();
        String textMessage = isHtml ? null : message;   // + "\n\n" + DMX_HOST_URL + "\n\n"     // TODO?
        String htmlMessage = isHtml ? message : null;   // + "\n\n" + DMX_HOST_URL + "\n\n"     // TODO?
        sendmail.doEmailRecipientAs(sender, projectName, subject, textMessage, htmlMessage, recipientValues);
    }

    private Assoc getDefaultAssociation(long topic1, long topic2) {
        return dmx.getAssocBetweenTopicAndTopic(ASSOCIATION,  topic1, topic2, DEFAULT, DEFAULT);
    }

    private Assoc getMembershipAssociation(long id, long idTwo) {
        return dmx.getAssocBetweenTopicAndTopic(MEMBERSHIP,  id, idTwo, DEFAULT, DEFAULT);
    }

    /**
     * The sign-up configuration object is loaded once when this bundle/plugin
     * is initialized by the framework and as soon as one configuration was
     * edited.
     *
     * @see #reloadAssociatedSignupConfiguration()
     */
    private ModuleConfiguration loadConfiguration() {
        // Fixme: ### Allow for multiple sign-up configuration topics to exist and one to be active (configured).
        return new ModuleConfiguration(dmx.getTopicByUri("dmx.signup.default_configuration"));
        /** 
        Topic pluginTopic = dmx.getTopicByUri(SIGNUP_SYMBOLIC_NAME);
        return pluginTopic.getRelatedTopic(ASSOCIATION, DEFAULT, DEFAULT,
                SIGN_UP_CONFIG_TYPE_URI); **/
    }

    @Override
    public ModuleConfiguration getConfiguration() {
        return activeModuleConfiguration;
    }

    @Override
    public List<String> getAuthorizationMethods() {
        Map<String, String> knownAms = new HashMap<>();
        Set<String> originalAms = new HashSet<>(accesscontrol.getAuthorizationMethods());
        originalAms.add("Basic");
        for (String s : originalAms) {
            // key: lowercased
            // value: original value
            knownAms.put(s.toLowerCase(), s);
        }
        List<String> filteredRestrictedAms = new ArrayList<>();
        if (CONFIG_RESTRICT_AUTH_METHODS.trim().length() > 0) {
            // filters out any values the platform does not know from the restriction list
            // and deliberately preserve the order of the restriction list
            for (String s : CONFIG_RESTRICT_AUTH_METHODS.split(",")) {
                String trimmedLowercase = s.trim().toLowerCase();
                String value = knownAms.get(trimmedLowercase);
                if (value != null) {
                    filteredRestrictedAms.add(value);
                }
            }
        } else {
            // Copy the original authorization methods
            filteredRestrictedAms.addAll(originalAms);
        }
        return filteredRestrictedAms;
    }

    private void transactional(Runnable r) {
        DMXTransaction tx = dmx.beginTx();
        try {
            r.run();
            tx.success();
        } catch (Throwable t) {
            log.warning("A custom transaction failed: " + t.getLocalizedMessage());
            tx.failure();
            throw t;
        } finally {
            tx.finish();
        }
    }
}
