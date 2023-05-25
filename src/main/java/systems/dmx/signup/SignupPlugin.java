package systems.dmx.signup;

import com.sun.jersey.core.util.Base64;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.core.Assoc;
import systems.dmx.core.Topic;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.EventListener;
import systems.dmx.core.service.*;
import systems.dmx.core.service.accesscontrol.AccessControlException;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.core.service.event.PostUpdateTopic;
import systems.dmx.facets.FacetsService;
import systems.dmx.ldap.service.LDAPPluginService;
import systems.dmx.sendmail.SendmailService;
import systems.dmx.signup.configuration.AccountCreation;
import systems.dmx.signup.configuration.ModuleConfiguration;
import systems.dmx.workspaces.WorkspacesService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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

    HashMap<String, JSONObject> token = new HashMap<String, JSONObject>();
    HashMap<String, JSONObject> pwToken = new HashMap<String, JSONObject>();

    EmailTextProducer emailTextProducer = new DefaultEmailTextProducer();

    @Override
    public void init() {
        initOptionalServices();
        reloadAssociatedSignupConfiguration();
        // Log configuration settings
        log.info("\n  dmx.signup.account_creation: " + CONFIG_ACCOUNT_CREATION + "\n"
            + "  dmx.signup.account_creation_password_handling: " + CONFIG_ACCOUNT_CREATION_PASSWORD_HANDLING + "\n"
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
     * If the username is already taken isAvailable is set to false.
     */
    @GET
    @Path("/check/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
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
    @Produces(MediaType.APPLICATION_JSON)
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

    /**
     * A HTTP Resource to initiate a password-reset sequence. Creates a password-reset token
     * and sends it out as link via Email. Redirects the request to either the "token info"
     * or the "error message" page.
     * @param email
     * @return A Response.temporaryRedirect to either the "token info"
     * or the "error message" page.
     * @throws URISyntaxException
     */
    @GET
    @Path("/password-token/{email}/{redirectUrl}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public Response initiateRedirectPasswordReset(@PathParam("email") String email,
            @PathParam("redirectUrl") String redirectUrl) throws URISyntaxException {
        log.info("Password reset requested for user with Email: \"" + email + "\" wishing to redirect to: \"" +
            redirectUrl + "\"");
        String emailAddressValue = email.trim();
        boolean emailExists = dmx.getPrivilegedAccess().emailAddressExists(emailAddressValue);
        if (emailExists) {
            log.info("Email based password reset workflow do'able, sending out passwort reset mail.");
            // ### Todo: Add/include return Url to token (!)
            // Note: Here system can't know "display name" (anonymous has
            // no read permission on it) and thus can't pass it on
            sendPasswordResetToken(emailAddressValue, null, redirectUrl);
            return Response.ok().build();
        }
        log.warning("Email based password reset workflow not do'able, Email Address does NOT EXIST => " + email.trim());
        return Response.serverError().build();
    }

    @Override
    public Response initiatePasswordResetWithName(String email, String name) throws URISyntaxException {
        return null;
    }

    @GET
    @Path("/self-registration-active")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSelfRegistrationStatus() {
        return Response.ok("" + isSelfRegistrationEnabled()).build();
    }

    /**
     * Updates the user password.
     * @param token
     * @param password
     * @return Returns the correct template for the input.
     */
    @GET
    @Path("/password-reset/{token}/{password}")
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public Response processAjaxPasswordUpdateRequest(@PathParam("token") String token,
                                                     @PathParam("password") String password) {
        log.info("Processing Password Update Request Token... ");
        try {
            JSONObject entry = pwToken.get(token);
            if (entry != null) {
                Credentials newCreds = new Credentials("dummy", "pass");
                newCreds.username = entry.getString("username").trim();
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
                        return Response.serverError().build();
                    }
                }
                pwToken.remove(token);
                return Response.ok().build();
            } else {
                return Response.serverError().build();
            }
        } catch (JSONException ex) {
            Logger.getLogger(SignupPlugin.class.getName()).log(Level.SEVERE, null, ex);
            return Response.serverError().build();
        }
    }

    /**
     * A HTTP resource for JS clients to create a new user account with a display name, email address as username and
     * password.
     *
     * Requires the currently logged in user to be a member of the administration workspace or a member of a designated
     * workspace (specified through the configuration).
     *
     * @param mailbox       String must be unique
     * @param displayName   String
     * @param password      String For LDAP window.btoa encoded and for DMX -SHA-256- encoded
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/custom-handle/{mailbox}/{displayname}/{password}")
    @Transactional
    @Override
    public Topic handleCustomAJAXSignupRequest(@PathParam("mailbox") String mailbox,
                                               @PathParam("displayname") String displayName,
                                               @PathParam("password") String password) throws URISyntaxException {
        checkAccountCreation();
        Topic username = createCustomUserAccount(mailbox, displayName, password); // throws if account creation fails
        log.info("Created new user account for user with display \"" + displayName + "\" and mailbox " + mailbox);
        return username;
    }

    @Override
    public Topic createCustomUserAccount(String mailbox, String displayName, String password) {
        try {
            // 1) Custom sign-up request means "mailbox" = "username"
            String username = createSimpleUserAccount(mailbox.trim(), password, mailbox.trim());
            // 2) create and assign displayname topic to "System" workspace
            final String displayNameValue = displayName.trim();
            final Topic usernameTopic = accesscontrol.getUsernameTopic(username);
            final long usernameTopicId = usernameTopic.getId();
            long displayNamesWorkspaceId = getDisplayNamesWorkspaceId();
            dmx.getPrivilegedAccess().runInWorkspaceContext(displayNamesWorkspaceId, new Callable<Topic>() {
                @Override
                public Topic call() {
                    // create display name facet for username topic
                    facets.addFacetTypeToTopic(usernameTopicId, DISPLAY_NAME_FACET);
                    facets.updateFacet(usernameTopicId, DISPLAY_NAME_FACET,
                            mf.newFacetValueModel(DISPLAY_NAME)
                                    .set(displayNameValue));
                    // automatically make users member in "Display Names" workspace
                    accesscontrol.createMembership(username, displayNamesWorkspaceId);
                    log.info("Created membership for new user account in \"Display Names\" "
                            + "workspace (SharingMode.Collaborative)");
                    // Account creator should be member of "Display Names" ..
                    // or is "runInWorkspacecContext privileged to GET?
                    return facets.getFacet(usernameTopicId, DISPLAY_NAME_FACET);
                }
            });
            return usernameTopic;
        } catch (Exception e) {
            throw new RuntimeException("Creating custom user account failed, mailbox=\"" + mailbox +
                "\", displayName=\"" + displayName + "\"", e);
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
                log.info("Revoke Request for API Workspace Membership by user \"" +
                    usernameTopic.getSimpleValue().toString() + "\"");
                String api_usage_revoked = emailTextProducer.getApiUsageRevokedMailSubject();
                String message = emailTextProducer.getApiUsageRevokedMailText(usernameTopic.getSimpleValue().toString());
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
                    String message = emailTextProducer.getAccountActiveEmailMessage(username.toString(), DMX_HOST_URL);
                    sendSystemMail(subject, message, mailboxValue);
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

    @Override
    public void sendSystemMailboxNotification(String subject, String message) {
        if (!CONFIG_ADMIN_MAILBOX.isEmpty()) {
            String recipient = CONFIG_ADMIN_MAILBOX;
            try {
                sendSystemMail(subject, message, recipient);
            } catch (Exception ex) {
                log.severe("There seems to be an issue with your mail (SMTP) setup, we FAILED sending out a " +
                    "notification mail to the \"System Mailbox\", caused by: " + ex.getMessage());
            }
        } else {
            log.info("Did not send notification mail to System Mailbox - Admin Mailbox Empty");
        }
    }

    @Override
    public void sendUserMailboxNotification(String mailbox, String subject, String message) {
        try {
            sendSystemMail(subject, message, mailbox);
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
    private boolean isLdapAccountCreationEnabled() {
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

    @Override
    public String createSimpleUserAccount(String username, String password, String mailbox) {
        try {
            if (isUsernameTaken(username)) {
                // Might be thrown if two users compete for registration (of the same username)
                // within the same 60 minutes (tokens validity timespan). First confirming, wins.
                throw new RuntimeException("Username was already registered and confirmed!");
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
            // 2) create and associate e-mail address topic in "System" Workspace
            long systemWorkspaceId = dmx.getPrivilegedAccess().getSystemWorkspaceId();
            dmx.getPrivilegedAccess().runInWorkspaceContext(systemWorkspaceId, new Callable<Topic>() {
                @Override
                public Topic call() {
                    Topic eMailAddress = dmx.createTopic(mf.newTopicModel(USER_MAILBOX_TYPE_URI,
                        new SimpleValue(eMailAddressValue)));
                    // 3) fire custom event ### this is useless since fired by "anonymous" (this request scope)
                    dmx.fireEvent(USER_ACCOUNT_CREATE_LISTENER, usernameTopic);
                    // 4) associate email address to "username" topic too
                    dmx.createAssoc(mf.newAssocModel(USER_MAILBOX_EDGE_TYPE,
                        mf.newTopicPlayerModel(eMailAddress.getId(), CHILD),
                        mf.newTopicPlayerModel(usernameTopic.getId(), PARENT)));
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

    @Override
    public boolean isValidEmailAddress(String value) {
        // ### Todo: Implement email-valid check into dmx-sendmail Service, utilizing
        // import javax.mail.internet.AddressException;
        // import javax.mail.internet.InternetAddress;
        return true;
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
        dmx.getTopic(workspaces.getWorkspace(CONFIG_ACCOUNT_CREATION_AUTH_WS_URI).getId())
                .checkWriteAccess();
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

    private void sendUserValidationToken(String username, String password, String mailbox) {
        String tokenKey = createUserValidationToken(username, password, mailbox);
        sendConfirmationMail(tokenKey, username, mailbox.trim());
    }

    private void sendPasswordResetToken(String mailbox, String displayName, String redirectUrl) {
        String username = dmx.getPrivilegedAccess().getUsername(mailbox);
        // Todo: Would need privileged access to "Display Name" to display it in password-update dialog
        String tokenKey = createPasswordResetToken(username, mailbox, displayName, redirectUrl);
        sendPasswordResetMail(tokenKey, username, mailbox.trim(), displayName);
    }

    private String createUserValidationToken(String username, String password, String mailbox) {
        try {
            String tokenKey = UUID.randomUUID().toString();
            long valid = calculateTokenExpiration();
            JSONObject tokenValue = new JSONObject()
                .put("username", username.trim())
                .put("mailbox", mailbox.trim())
                .put("password", password)
                .put("expiration", valid);
            token.put(tokenKey, tokenValue);
            log.log(Level.INFO, "Set up key {0} for {1} sending confirmation mail valid till {3}",
                    new Object[]{tokenKey, mailbox, new Date(valid).toString()});
            // ### TODO: if sending confirmation mail fails users should know about that and
            // get to see the "failure" screen next (with a proper message)
            return tokenKey;
        } catch (JSONException ex) {
            Logger.getLogger(SignupPlugin.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private long calculateTokenExpiration() {
        return Instant.now().plus(CONFIG_TOKEN_EXPIRATION_DURATION).toEpochMilli();
    }

    private String createPasswordResetToken(String username, String mailbox, String name, String redirectUrl) {
        try {
            String tokenKey = UUID.randomUUID().toString();
            long valid = calculateTokenExpiration();
            JSONObject tokenValue = new JSONObject()
                .put("username", username.trim())
                .put("mailbox", mailbox.trim())
                .put("name", (name != null) ? name.trim() : "")
                .put("expiration", valid)
                .put("redirectUrl", redirectUrl);
            pwToken.put(tokenKey, tokenValue);
            log.log(Level.INFO, "Set up pwToken {0} for {1} send passwort reset mail valid till {3}",
                new Object[]{tokenKey, mailbox, new Date(valid).toString()});
            return tokenKey;
        } catch (JSONException ex) {
            Logger.getLogger(SignupPlugin.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
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
        log.log(Level.INFO, "Sign-up Configuration Loaded (URI=\"{0}\"), Name=\"{1}\"",
            new Object[]{
                    activeModuleConfiguration.getConfigurationUri(),
                    activeModuleConfiguration.getConfigurationName()});

    }

    private void sendConfirmationMail(String key, String username, String mailbox) {
        try {
            URL url = new URL(DMX_HOST_URL);
            log.info("The confirmation mails token request URL should be:" + "\n" + url + "sign-up/confirm/" + key);
            // Localize "sentence" structure for german, maybe via Formatter
            try {
                String href = url + "sign-up/confirm/" + key;
                if (DMX_ACCOUNTS_ENABLED) {
                    String mailSubject = emailTextProducer.getConfirmationActiveMailSubject();
                    String message = emailTextProducer.getConfirmationActiveMailMessage(username, href);
                    sendSystemMail(mailSubject, message, mailbox
                    );
                } else {
                    String mailSubject = emailTextProducer.getConfirmationProceedMailSubject();
                    String message = emailTextProducer.getUserConfirmationProceedMailMessage(username, href);
                    sendSystemMail(mailSubject, message, mailbox);
                }
            } catch (Exception ex) {
                log.severe("There seems to be an issue with your mail (SMTP) setup, we FAILED sending out the " +
                    "\"Email Confirmation\" mail, caused by: " + ex.getMessage());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void sendPasswordResetMail(String key, String username, String mailbox, String displayName) {
        try {
            URL url = new URL(DMX_HOST_URL);
            String href = url + "sign-up/password-reset/" + key;
            log.info("The password reset mails token request URL should be: \n" + href);
            try {
                String addressee = username;
                if (displayName != null && !displayName.isEmpty()) {
                    addressee = displayName;
                }
                String subject = emailTextProducer.getPasswordResetMailSubject();
                String message = emailTextProducer.getPasswordResetMailMessage(href, addressee);
                sendSystemMail(subject,
                        message, mailbox);
            } catch (Exception ex) {
                log.severe("There seems to be an issue with your mail (SMTP) setup, we FAILED sending out the " +
                    "\"Password Reset\" mail, caused by: " + ex.getMessage());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void sendNotificationMail(String username, String mailbox) {
        if (CONFIG_ADMIN_MAILBOX != null && !CONFIG_ADMIN_MAILBOX.isEmpty()) {
            try {
                String subject = emailTextProducer.getAccountCreationSystemEmailSubject();
                String message = emailTextProducer.getAccountCreationSystemEmailMessage(username, mailbox);
                sendSystemMail(subject,
                        message, CONFIG_ADMIN_MAILBOX);
            } catch (Exception ex) {
                log.severe("There seems to be an issue with your mail (SMTP) setup, we FAILED notifying the " +
                    "\"system mailbox\" about account creation, caused by: " + ex.getMessage());
            }
        } else {
            log.info("ADMIN: No \"Admin Mailbox\" configured: A new user account (" + username + ") was created but" +
                " no notification could be sent.");
        }
    }

    /**
     *
     * @param subject       String Subject text for the message.
     * @param message       String Text content of the message.
     * @param recipientValues     String of Email Address message is sent to **must not** be NULL.
     */
    @Override
    public void sendSystemMail(String subject, String message, String recipientValues) {
        String projectName = "TODO"; // TODO
        //String projectName = activeModuleConfiguration.getProjectTitle();
        String sender = CONFIG_FROM_MAILBOX;
        String mailBody = message; // + "\n\n" + DMX_HOST_URL + "\n\n"
        sendmail.doEmailRecipientAs(sender, projectName, subject, mailBody, recipientValues);
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
        Set<String> originalAms = new HashSet(accesscontrol.getAuthorizationMethods());
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

}
