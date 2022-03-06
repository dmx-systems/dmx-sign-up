package systems.dmx.signup;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.util.Base64;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.framework.Bundle;
import org.thymeleaf.context.AbstractContext;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.core.Assoc;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.Topic;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.EventListener;
import systems.dmx.core.service.*;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.core.service.event.PostUpdateTopic;
import systems.dmx.core.storage.spi.DMXTransaction;
import systems.dmx.ldap.service.LDAPPluginService;
import systems.dmx.sendmail.SendmailService;
import systems.dmx.signup.events.SignupResourceRequestedListener;
import systems.dmx.signup.service.SignupPluginService;
import systems.dmx.thymeleaf.ThymeleafPlugin;
import systems.dmx.workspaces.WorkspacesService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static systems.dmx.accesscontrol.Constants.*;
import static systems.dmx.core.Constants.*;
import static systems.dmx.signup.Constants.*;
import static systems.dmx.workspaces.Constants.WORKSPACE;

/**
 * This plugin enables anonymous users to create themselves a user account in DMX
 * through an (optional) Email based confirmation workflow and thus it depends on the dmx-sendmail plugin and e.g. postfix
 * like "internet" installation for "localhost". Source code available at: https://git.dmx.systems/dmx-plugins/dmx-sign-up
 * @version 2.0.0-SNAPSHOT
 * @author Malte Rei&szlig;
**/
@Path("/sign-up")
public class SignupPlugin extends ThymeleafPlugin implements SignupPluginService, PostUpdateTopic {

    private static Logger log = Logger.getLogger(SignupPlugin.class.getName());
 
    private Topic activeModuleConfiguration = null;
    private Topic customWorkspaceAssignmentTopic = null;
    private String systemEmailContact = null;
    private ResourceBundle rb = null;

    @Inject
    private AccessControlService acService;
    @Inject
    private SendmailService sendmail;
    @Inject
    private WorkspacesService wsService; // Used in migrations
    @Inject
    private LDAPPluginService ldapPluginService;

    @Context
    UriInfo uri;

    HashMap<String, JSONObject> token = new HashMap<String, JSONObject>();
    HashMap<String, JSONObject> pwToken = new HashMap<String, JSONObject>();

    @Override
    public void init() {
        initTemplateEngine();
        loadPluginLanguageProperty();
        reloadAssociatedSignupConfiguration();
        // Log configuration settings
        log.info("\n  dmx.signup.self_registration: " + CONFIG_SELF_REGISTRATION + "\n"
            + "  dmx.signup.confirm_email_address: " + CONFIG_EMAIL_CONFIRMATION + "\n"
            + "  dmx.signup.admin_mailbox: " + CONFIG_ADMIN_MAILBOX + "\n"
            + "  dmx.signup.system_mailbox: " + CONFIG_FROM_MAILBOX + "\n"
            + "  dmx.signup.ldap_account_creation: " + CONFIG_CREATE_LDAP_ACCOUNTS);
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

    /**
     * Custom event fired by sign-up module whenever a template resource is requested.
     **/
    static DMXEvent SIGNUP_RESOURCE_REQUESTED = new DMXEvent(SignupResourceRequestedListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((SignupResourceRequestedListener) listener).signupResourceRequested((AbstractContext) params[0], (String) params[1]);
        }
    };



    // --- Plugin Service Implementation --- //

    /** 
     * Fetch all ui-labels in the language the plugins source code was compiled.
     * @param language
     * @return A String containing a JSONObject with key-value pairs of all multilingual labels.
     */
    @GET
    @Path("/translation/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTranslationTable(@PathParam("locale") String language) {
        if (language.isEmpty()) return null;
        Locale le = new Locale(language);
        ResourceBundle newRb = ResourceBundle.getBundle("SignupMessages", le);
        Enumeration bundleKeys = newRb.getKeys();
        JSONObject response = new JSONObject();
        while (bundleKeys.hasMoreElements()) {
            try {
                String key = (String) bundleKeys.nextElement();
                response.put(key, newRb.getString(key));
            } catch (JSONException ex) {
                Logger.getLogger(SignupPlugin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return response.toString();
    }

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/displayname/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public String getDisplayName(@PathParam("username") String username) {
        Topic usernameTopic = dmx.getTopicByValue(USERNAME, new SimpleValue(username));
        Topic displayName = usernameTopic.getRelatedTopic(SIGNUP_DISPLAY_NAME_EDGE, DEFAULT, DEFAULT, SIGNUP_NAME);
        return (displayName != null) ? displayName.getSimpleValue().toString() : username;
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
    @Path("/password-token/{email}")
    @Produces(MediaType.TEXT_HTML)
    @Override
    public Response initiatePasswordReset(@PathParam("email") String email) throws URISyntaxException {
        log.info("Password reset requested for user with Email: \"" + email + "\"");
        try {
            String emailAddressValue = email.trim();
            boolean emailExists = dmx.getPrivilegedAccess().emailAddressExists(emailAddressValue);
            if (emailExists) {
                log.info("Email based password reset workflow do'able, sending out passwort reset mail.");
                sendPasswordResetToken(emailAddressValue);
                return Response.temporaryRedirect(new URI("/sign-up/token-info")).build();
            } else {
                log.info("Email based password reset workflow not do'able, Email Address does NOT EXIST => " + email.trim());
            }
        } catch (URISyntaxException ex) {
            Logger.getLogger(SignupPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Response.temporaryRedirect(new URI("/sign-up/error")).build();
    }

    @GET
    @Path("/self-registration-active")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSelfRegistrationStatus() {
        return Response.ok("" + CONFIG_SELF_REGISTRATION).build();
    }

    /** 
     * Checks the given password-reset token for validity and return either the
     * password-reset dialog or the error message page.
     * @param token
     * @return The correct dialog/template for the given password-reset token value.
     */
    @GET
    @Path("/password-reset/{token}")
    @Produces(MediaType.TEXT_HTML)
    public Viewable handlePasswordResetRequest(@PathParam("token") String token) {
        try {
            // 1) Assert token exists: It may not exist due to e.g. bundle refresh, system restart, token invalid
            if (!pwToken.containsKey(token)) {
                viewData("message", rb.getString("link_invalid"));
            }
            // 2) Process available token and remove it from stack
            String username, email;
            JSONObject input = pwToken.get(token);
            // 3) Update the user account credentials OR present an error message.
            viewData("token", token);
            if (input != null && input.getLong("expiration") > new Date().getTime()) {
                username = input.getString("username");
                email = input.getString("mailbox");
                log.info("Handling password reset request for Email: \"" + email);
                viewData("requested_username", username);
                viewData("password_requested_title", rb.getString("password_requested_title"));
                prepareSignupPage("password-reset");
                return view("password-reset");
            } else {
                log.warning("Sorry the link to reset the password for ... has expired.");
                viewData("message", rb.getString("reset_link_expired"));
                return getFailureView("updated");
            }
        } catch (JSONException ex) {
            log.severe("Sorry, an error occured during retriving your token. Please try again. " + ex.getMessage());
            viewData("message", rb.getString("reset_link_error"));
            return getFailureView("updated");
        }
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
    public Viewable processPasswordUpdateRequest(@PathParam("token") String token, @PathParam("password") String password) {
        log.info("Processing Password Update Request Token... ");
        try {
            JSONObject entry = pwToken.get(token);
            if (entry != null) {
                Credentials newCreds = new Credentials("dummy", "pass");
                newCreds.username = entry.getString("username").trim();
                newCreds.password = password;
                if (!ldapAccountCreationConfigured()) {
                    // Change password stored in "User Account" topic
                    dmx.getPrivilegedAccess().changePassword(newCreds);
                } else {
                    // LDAP requires plaintext password in credentials
                    String plaintextPassword = Base64.base64Decode(password);
                    newCreds.plaintextPassword = plaintextPassword;
                    newCreds.password = plaintextPassword;
                    ldapPluginService.changePassword(newCreds);
                }
                pwToken.remove(token);
                log.info("Credentials for user " + newCreds.username + " were changed succesfully.");
                viewData("message", rb.getString("reset_password_ok"));
                prepareSignupPage("password-ok");
                return view("password-ok");
            } else {
                viewData("message", rb.getString("reset_password_error"));
                return getFailureView("updated");
            }
        } catch (JSONException ex) {
            Logger.getLogger(SignupPlugin.class.getName()).log(Level.SEVERE, null, ex);
            viewData("message", rb.getString("reset_password_error"));
            return getFailureView("updated");
        }
    }

    /**
     * A HTTP resource to create a new user account.
     * @param username  String must be unique
     * @param password  String must be SHA-256 encoded
     * @param mailbox   String must be unique
     * @param skipConfirmation  Flag if "true" skips intiating the email verification process
     * (useful to allow admins to create new accounts without verifying users).
     * @return 
     */
    @GET
    @Path("/handle/{username}/{pass-one}/{mailbox}/{skipConfirmation}")
    @Override
    public Viewable handleSignupRequest(@PathParam("username") String username, @PathParam("pass-one") String password,
                                        @PathParam("mailbox") String mailbox,
                                        @PathParam("skipConfirmation") boolean skipConfirmation) {
        if (!CONFIG_SELF_REGISTRATION & !isAdministrationWorkspaceMember()) {
            throw new WebApplicationException(Response.noContent().build());
        }
        try {
            if (CONFIG_EMAIL_CONFIRMATION) {
                if (skipConfirmation && isAdministrationWorkspaceMember()) {
                    log.info("Sign-up Configuration: Email based confirmation workflow active, Administrator skipping confirmation mail.");
                    createSimpleUserAccount(username, password, mailbox);
                    handleAccountCreatedRedirect(username);
                } else {
                    log.info("Sign-up Configuration: Email based confirmation workflow active, send out confirmation mail.");
                    sendUserValidationToken(username, password, mailbox);
                    // redirect user to a "token-info" page
                    throw new WebApplicationException(Response.temporaryRedirect(new URI("/sign-up/token-info")).build());
                }
            } else {
                createSimpleUserAccount(username, password, mailbox);
                handleAccountCreatedRedirect(username);
            }
        } catch (URISyntaxException e) {
            log.log(Level.SEVERE, "Could not build response URI while handling sign-up request", e);
        }
        return getFailureView("created");
    }

    /**
     * A HTTP resource to create a new user account.
     * @param username  String must be unique
     * @param password  String must be SHA-256 encoded
     * @param mailbox   String must be unique
     * @return 
     */
    @GET
    @Path("/handle/{username}/{pass-one}/{mailbox}")
    @Override
    public Viewable handleSignupRequest(@PathParam("username") String username,
            @PathParam("pass-one") String password, @PathParam("mailbox") String mailbox) {
        return handleSignupRequest(username, password, mailbox, false);
    }

    /**
     * A HTTP resource to create a new user account with a display name, email address as username and some random password.
     * @param mailbox  String must be unique
     * @param displayName String
     * @return
     */
    @GET
    @Path("/custom-handle/{mailbox}/{displayname}/{password}")
    public Viewable handleCustomSignupRequest(@PathParam("mailbox") String mailbox,
            @PathParam("displayname") String displayName, @PathParam("password") String password) throws URISyntaxException {
        // 1) Todo: ### generate random password
        String username = createSimpleUserAccount(mailbox, password, mailbox);
        // 2) create and assign displayname topic to "System" workspace
        final String displayNameValue = displayName;
        // unauthenticated/not-logged in user has no permission to fetch username (despite it should be, as it reside in System ws))?
        final Topic usernameTopic = dmx.getTopicByValue(USERNAME, new SimpleValue(username));
        final long usernameTopicId = usernameTopic.getId();
        DMXTransaction tx = dmx.beginTx();
        try {
            dmx.getPrivilegedAccess().runInWorkspaceContext(-1, new Callable<Topic>() {
                @Override
                public Topic call() {
                    long systemWorkspaceId = dmx.getPrivilegedAccess().getSystemWorkspaceId();
                    Topic displayName = dmx.createTopic(mf.newTopicModel(SIGNUP_NAME, new SimpleValue(displayNameValue)));
                    dmx.getPrivilegedAccess().assignToWorkspace(displayName, systemWorkspaceId);
                    Assoc assoc = dmx.createAssoc(mf.newAssocModel(SIGNUP_DISPLAY_NAME_EDGE,
                        mf.newTopicPlayerModel(displayName.getId(), DEFAULT),
                        mf.newTopicPlayerModel(usernameTopicId, DEFAULT)));
                    dmx.getPrivilegedAccess().assignToWorkspace(assoc, systemWorkspaceId);
                    if (customWorkspaceAssignmentTopic != null) {
                        acService.createMembership(usernameTopic.getSimpleValue().toString(),
                                customWorkspaceAssignmentTopic.getId());
                        log.info("Created new Membership for " + usernameTopic.getSimpleValue().toString() + " in " +
                                "workspace=" + customWorkspaceAssignmentTopic.getSimpleValue().toString());
                    }
                    return displayName;
                }
            });
            tx.success();
        } catch (Exception e) {
            tx.failure();
            throw new RuntimeException("Creating simple user account FAILED!", e);
        } finally {
            tx.finish();
        }
        log.info("Created new user account for user with display \"" + displayName + "\" and mailbox " + mailbox);
        handleAccountCreatedRedirect(username); // hrows WebAppException to issue a redirect (thus method not @Transactional)
        return getFailureView("created");
    }

    /**
     * The HTTP resource to confirm the email address and acutally create an account.
     * @param key String must be a valid token
     * @return 
     */
    @GET
    @Path("/confirm/{token}")
    public Viewable processSignupRequest(@PathParam("token") String key) {
        // 1) Assert token exists: It may not exist due to e.g. bundle refresh, system restart, token invalid
        if (!token.containsKey(key)) {
            viewData("username", null);
            viewData("message", rb.getString("link_invalid"));
            return getFailureView("created");
        }
        // 2) Process available token and remove it from stack
        String username;
        JSONObject input = token.get(key);
        token.remove(key);
        // 3) Create the user account and show ok OR present an error message.
        try {
            username = input.getString("username");
            if (input.getLong("expiration") > new Date().getTime()) {
                log.log(Level.INFO, "Trying to create user account for {0}", input.getString("mailbox"));
                createSimpleUserAccount(username, input.getString("password"), input.getString("mailbox"));
            } else {
                viewData("username", null);
                viewData("message", rb.getString("link_expired"));
                return getFailureView("created");
            }
        } catch (JSONException ex) {
            Logger.getLogger(SignupPlugin.class.getName()).log(Level.SEVERE, null, ex);
            viewData("message", rb.getString("internal_error"));
            log.log(Level.SEVERE, "Account creation failed due to {0} caused by {1}",
                new Object[]{ex.getMessage(), ex.getCause().toString()});
            return getFailureView("created");
        }
        log.log(Level.INFO, "Account succesfully created for username: {0}", username);
        viewData("message", rb.getString("account_created"));
        if (!DMX_ACCOUNTS_ENABLED) {
            log.log(Level.INFO, "> Account activation by an administrator remains PENDING ");
            return getAccountCreationPendingView();
        }
        return getAccountCreationOKView(username);
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
        if (apiMembershipRequestNote != null && acService.getUsername() != null) {
            Topic usernameTopic = acService.getUsernameTopic(acService.getUsername());
            // 1) Try to manage workspace membership directly (success depends on ACL and the SharingMode of the configured workspace)
            createApiWorkspaceMembership(usernameTopic); // might fail silently
            // 2) Store API Membership Request in a Note (residing in the "System" workspace) association
            Assoc requestRelation = getDefaultAssociation(usernameTopic.getId(), apiMembershipRequestNote.getId());
            if (requestRelation == null) {
                // ### Fixme: For the moment it depends on (your web application, more specifically) the workspace cookie
                // set (at the requesting client) which workspace this assoc will be assigned to
                createApiMembershipRequestNoteAssociation(usernameTopic, apiMembershipRequestNote);
            } else {
                log.info("Revoke Request for API Workspace Membership by user \"" + usernameTopic.getSimpleValue().toString() + "\"");
                sendSystemMailboxNotification("API Usage Revoked", "<br/>Hi admin,<br/><br/>"
                    + usernameTopic.getSimpleValue().toString() + " just revoked his/her acceptance to your Terms of Service for API-Usage."
                            + "<br/><br/>Just wanted to let you know.<br/>Cheers!");
                // 2.1) fails in all cases where user has no write access to the workspace the association was created in
                // dmx.deleteAssociation(requestRelation.getId());
                // For now: API Usage Membership must be revoked per Email but personally and confirmed by the administrator
                // A respective hint was place in the "API Usage" dialog on the users account (/sign-up/edit) page.
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
            Topic username = topic.getRelatedTopic("dmx.config.configuration", null,
                    null, USERNAME);
            // Perform notification
            if (status && !DMX_ACCOUNTS_ENABLED) { // Enabled=true && new_accounts_are_enabled=false
                log.info("Sign-up Notification: User Account \"" + username.getSimpleValue()+"\" is now ENABLED!");
                //
                String webAppTitle = activeModuleConfiguration.getChildTopics().getTopic(CONFIG_WEBAPP_TITLE)
                        .getSimpleValue().toString();
                Topic mailbox = username.getRelatedTopic(USER_MAILBOX_EDGE_TYPE, null, null, USER_MAILBOX_TYPE_URI);
                if (mailbox != null) { // for accounts created via sign-up plugin this will always evaluate to true
                    String mailboxValue = mailbox.getSimpleValue().toString();
                    sendSystemMail("Your account on " + webAppTitle + " is now active",
                            rb.getString("mail_hello") + " " + username.getSimpleValue()
                                    + ",<br/><br/>your account on <a href=\"" + DMX_HOST_URL + "\">" + webAppTitle + "</a> is now " +
                                    "active.<br/><br/>" + rb.getString("mail_ciao"), mailboxValue);
                    log.info("Send system notification mail to " + mailboxValue + " - The account is now active!");
                }
            }
        }
    }

    // --- Sign-up Plugin Routes --- //

    /**
     * The root resource, routing either the sign-up or the logout dialog.
     * @return 
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getSignupFormView() throws URISyntaxException {
        if (!CONFIG_SELF_REGISTRATION && !isAdministrationWorkspaceMember()) {
            throw new WebApplicationException(Response.temporaryRedirect(new URI("/systems.dmx.webclient/")).build());
        }
        if (acService.getUsername() != null && !isAdministrationWorkspaceMember()) {
            prepareSignupPage("logout");
            return view("logout");
        }
        prepareSignupPage("sign-up");
        return view("sign-up");
    }

    /**
     * The login resource, routing either the login or the logout dialog.
     * @return 
     */
    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getLoginView() {
        if (acService.getUsername() != null) {
            prepareSignupPage("logout");
            return view("logout");
        }
        prepareSignupPage("login");
        return view("login");
    }

    /**
     * The route for the password forgotton page to initiate a reset sequence.
     * @return 
     */
    @GET
    @Path("/request-password")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getPasswordResetView() {
        prepareSignupPage("request-password");
        return view("request-password");
    }

    /**
     * The route to the confirmation page for the account creation.
     * @param username
     * @return 
     */
    @GET
    @Path("/{username}/ok")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getAccountCreationOKView(@PathParam("username") String username) {
        prepareSignupPage("ok");
        viewData("requested_username", username);
        return view("ok");
    }

    /**
     * The route to the pending page, informing the user to wait for an
     * administrator to activate the account.
     * @return 
     */
    @GET
    @Path("/pending")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getAccountCreationPendingView() {
        prepareSignupPage("pending");
        return view("pending");
    }

    /**
     * The route to the error message page.
     * @return 
     */
    @GET
    @Path("/error")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getFailureView() {
        return getFailureView(null);
    }

    private Viewable getFailureView(String status) {
        if (status != null && status.equals("created")) {
            viewData("status_label", rb.getString("status_label_created"));
        } else {
            viewData("status_label", rb.getString("status_label_updated"));
        }
        viewData("account_failure_message", rb.getString("account_failure_message"));
        viewData("please_try_1", rb.getString("please_try_1"));
        viewData("please_try_2", rb.getString("please_try_2"));
        viewData("please_try_3", rb.getString("please_try_3"));
        prepareSignupPage("failure");
        return view("failure");
    }

    /**
     * The route informing users to please check their mail in
     * the next 60mins to verify and activate their new account.
     * @return 
     */
    @GET
    @Path("/token-info")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getConfirmationInfoView() {
        prepareSignupPage("account-confirmation");
        return view("account-confirmation");
    }

    /**
     * The route to the users account edit page.
     * @return 
     */
    @GET
    @Path("/edit")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getAccountDetailsView() {
        prepareSignupPage("account-edit");
        prepareAccountEditPage();
        return view("account-edit");
    }

    @Override
    public void sendSystemMailboxNotification(String subject, String message) {
        if (!CONFIG_ADMIN_MAILBOX.isEmpty()) {
            String recipient = CONFIG_ADMIN_MAILBOX;
            try {
                sendSystemMail(subject, message, recipient);
            } catch (Exception ex) {
                log.severe("There seems to be an issue with your mail (SMTP) setup,"
                        + "we FAILED sending out a notification mail to the \"System Mailbox\", caused by: " +  ex.getMessage());
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
            log.severe("There seems to be an issue with your mail (SMTP) setup,"
                    + "we FAILED sending out a notification mail to User \""+mailbox+"\", caused by: " +  ex.getMessage());
        }
    }

    private boolean ldapAccountCreationConfigured() {
        return CONFIG_CREATE_LDAP_ACCOUNTS;
    }

    private Topic createUsername(Credentials credentials) throws Exception {
        if (ldapAccountCreationConfigured()) {
            return ldapPluginService.createUser(credentials);
        } else {
            return acService._createUserAccount(credentials);
        }
    }

    @Override
    public String createSimpleUserAccount(String username, String password, String mailbox) {
        DMXTransaction tx = dmx.beginTx();
        try {
            if (isUsernameTaken(username)) {
                // Might be thrown if two users compete for registration (of the same username)
                // within the same 60 minutes (tokens validity timespan). First confirming, wins.
                throw new RuntimeException("Username was already registered and confirmed!");
            }
            Credentials creds;
            // When the "Basic" method is used the password is already in -SHA256- form for all other
            // methods it is simply base64-encoded
            if (!ldapAccountCreationConfigured()) {
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
            // 2) create and associate e-mail address topic in "Administration" Workspace
            dmx.getPrivilegedAccess().runInWorkspaceContext(-1, new Callable<Topic>() {
                @Override
                public Topic call() {
                    long systemWorkspace = dmx.getPrivilegedAccess().getSystemWorkspaceId();
                    Topic eMailAddress = dmx.createTopic(mf.newTopicModel(USER_MAILBOX_TYPE_URI,
                        new SimpleValue(eMailAddressValue)));
                    // 3) fire custom event ### this is useless since fired by "anonymous" (this request scope)
                    dmx.fireEvent(USER_ACCOUNT_CREATE_LISTENER, usernameTopic);
                    dmx.getPrivilegedAccess().assignToWorkspace(eMailAddress, systemWorkspace);
                    // 4) associate email address to "username" topic too
                    Assoc assoc = dmx.createAssoc(mf.newAssocModel(USER_MAILBOX_EDGE_TYPE,
                        mf.newTopicPlayerModel(eMailAddress.getId(), CHILD),
                        mf.newTopicPlayerModel(usernameTopic.getId(), PARENT)));
                    dmx.getPrivilegedAccess().assignToWorkspace(assoc, systemWorkspace);
                    // 5) create membership to custom workspace topic
                    if (customWorkspaceAssignmentTopic != null) {
                        acService.createMembership(usernameTopic.getSimpleValue().toString(),
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
            tx.success();
            return username;
        } catch (Exception e) {
            throw new RuntimeException("Creating simple user account FAILED!", e);
        } finally {
            tx.finish();
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
        Topic userNameTopic = acService.getUsernameTopic(value);
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

    private void handleAccountCreatedRedirect(String username) throws URISyntaxException {
        if (DMX_ACCOUNTS_ENABLED) {
            log.info("DMX Config: The new account is now ENABLED, redirecting to OK page.");
            // redirecting user to the "your account is now active" page
            throw new WebApplicationException(Response.temporaryRedirect(new URI("/sign-up/"+username+"/ok")).build());
        } else {
            log.info("DMX Config: The new account is now DISABLED, redirecting to PENDING page.");
            // redirecting to page displaying "your account was created but needs to be activated"
            throw new WebApplicationException(Response.temporaryRedirect(new URI("/sign-up/pending")).build());
        }
    }

    private boolean isAdministrationWorkspaceMember() {
        String username = acService.getUsername();
        if (username != null) {
            long administrationWorkspaceId = dmx.getPrivilegedAccess().getAdminWorkspaceId();
            if (acService.isMember(username, administrationWorkspaceId)
                || acService.getWorkspaceOwner(administrationWorkspaceId).equals(username)) {
                return true;
            }
        }
        return false;
    }

    private boolean isApiWorkspaceMember() {
        String username = acService.getUsername();
        if (username != null) {
            String apiWorkspaceUri = activeModuleConfiguration.getChildTopics().getString(CONFIG_API_WORKSPACE_URI);
            if (!apiWorkspaceUri.isEmpty() && !apiWorkspaceUri.equals("undefined")) {
                Topic apiWorkspace = dmx.getPrivilegedAccess().getWorkspace(apiWorkspaceUri);
                if (apiWorkspace != null) {
                    return acService.isMember(username, apiWorkspace.getId());
                }
            } else {
                Topic usernameTopic = acService.getUsernameTopic();
                Topic apiMembershipRequestNote = dmx.getTopicByUri("dmx.signup.api_membership_requests");
                Assoc requestRelation = getDefaultAssociation(usernameTopic.getId(), apiMembershipRequestNote.getId());
                if (requestRelation != null) return true;
            }
        }
        return false;
    }

    private void sendUserValidationToken(String username, String password, String mailbox) {
        String tokenKey = createUserValidationToken(username, password, mailbox);
        sendConfirmationMail(tokenKey, username, mailbox.trim());
    }

    private void sendPasswordResetToken(String mailbox) {
        String username = dmx.getPrivilegedAccess().getUsername(mailbox);
        String tokenKey = createPasswordResetToken(username, mailbox);
        sendPasswordResetMail(tokenKey, username, mailbox.trim());
    }

    private String createUserValidationToken(String username, String password, String mailbox) {
        try {
            String tokenKey = UUID.randomUUID().toString();
            long valid = new Date().getTime() + 3600000; // Token is valid fo 60 min
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

    private String createPasswordResetToken(String username, String mailbox) {
        try {
            String tokenKey = UUID.randomUUID().toString();
            long valid = new Date().getTime() + 3600000; // Token is valid fo 60 min
            JSONObject tokenValue = new JSONObject()
                    .put("username", username.trim())
                    .put("mailbox", mailbox.trim())
                    .put("expiration", valid);
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
        log.info("Request for new custom API Workspace Membership by user \"" + usernameTopic.getSimpleValue().toString() + "\"");
        sendSystemMailboxNotification("API Usage Requested", "<br/>Hi admin,<br/><br/>"
            + usernameTopic.getSimpleValue().toString() + " accepted the Terms of Service for API Usage."
                    + "<br/><br/>Just wanted to let you know.<br/>Cheers!");
    }

    private void createApiWorkspaceMembership(Topic usernameTopic) {
        String apiWorkspaceUri = activeModuleConfiguration.getChildTopics().getString(CONFIG_API_WORKSPACE_URI);
        if (!apiWorkspaceUri.isEmpty() && !apiWorkspaceUri.equals("undefined")) { // do not rely or use this option in production
            Topic apiWorkspace = dmx.getPrivilegedAccess().getWorkspace(apiWorkspaceUri);
            if (apiWorkspace != null) {
                log.info("Request for new custom API Workspace Membership by user \""
                        + usernameTopic.getSimpleValue().toString() + "\"");
                // Attempt to create a Workspace membership for this Assocation/Relation
                acService.createMembership(usernameTopic.getSimpleValue().toString(), apiWorkspace.getId());
            } else {
                log.info("Revoke Request for API Workspace Membership by user \"" + usernameTopic.getSimpleValue().toString() + "\"");
                if (acService.isMember(usernameTopic.getSimpleValue().toString(), apiWorkspace.getId())) {
                    Assoc assoc = getMembershipAssociation(usernameTopic.getId(), apiWorkspace.getId());
                    dmx.deleteAssoc(assoc.getId());
                } else {
                    log.info("Skipped Revoke Request for non-existent API Workspace Membership for \""
                            + usernameTopic.getSimpleValue().toString() + "\"");
                }
            }
        } else {
            log.info("No API Workspace Configured: You must enter the URI of a programmatically created workspace topic"
                + " into your current \"Signup Configuration\".");
        }
    }

    /**
     * Loads the sign-up configuration, a topic of type "Sign-up Configuration" associated to this plugins
     * topic of type "Plugin".
     *
     * @see init()
     * @see postUpdateTopic()
     */
    private Topic reloadAssociatedSignupConfiguration() {
        // load module configuration
        activeModuleConfiguration = getCurrentSignupConfiguration();
        if (activeModuleConfiguration == null) {
            log.warning("Could not load associated Sign-up Plugin Configuration Topic during init/postUpdate");
            return null;
        }
        activeModuleConfiguration.loadChildTopics();
        // check for custom workspace assignment
        customWorkspaceAssignmentTopic = getCustomWorkspaceAssignmentTopic();
        if (customWorkspaceAssignmentTopic != null) {
            log.info("Configured Custom Sign-up Workspace => \""
                    + customWorkspaceAssignmentTopic.getSimpleValue() + "\"");
        }
        log.log(Level.INFO, "Sign-up Configuration Loaded (URI=\"{0}\"), Name=\"{1}\"",
            new Object[]{activeModuleConfiguration.getUri(), activeModuleConfiguration.getSimpleValue()});
        return activeModuleConfiguration;
    }

    private void sendConfirmationMail(String key, String username, String mailbox) {
        try {
            String webAppTitle = activeModuleConfiguration.getChildTopics().getString(CONFIG_WEBAPP_TITLE);
            URL url = new URL(DMX_HOST_URL);
            log.info("The confirmation mails token request URL should be:"
                + "\n" + url + "sign-up/confirm/" + key);
            // Localize "sentence" structure for german, maybe via Formatter
            String mailSubject = rb.getString("mail_confirmation_subject") + " - " + webAppTitle;
            try {
                String linkHref = "<a href=\"" + url + "sign-up/confirm/" + key + "\">"
                    + rb.getString("mail_confirmation_link_label") + "</a>";
                if (DMX_ACCOUNTS_ENABLED) {
                    sendSystemMail(mailSubject,
                        rb.getString("mail_hello") + " " + username + ",<br/><br/>"
                            +rb.getString("mail_confirmation_active_body")+"<br/><br/>"
                            + linkHref + "<br/><br/>" + rb.getString("mail_ciao"), mailbox);
                } else {
                    sendSystemMail(mailSubject,
                        rb.getString("mail_hello") + " " + username + ",<br/><br/>"
                            + rb.getString("mail_confirmation_proceed_1")+"<br/>"
                            + linkHref + "<br/><br/>" + rb.getString("mail_confirmation_proceed_2")
                            + "<br/><br/>" + rb.getString("mail_ciao"), mailbox);
                }
            } catch (Exception ex) {
                log.severe("There seems to be an issue with your mail (SMTP) setup,"
                        + "we FAILED sending out the \"Email Confirmation\" mail, caused by: " +  ex.getMessage());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void sendPasswordResetMail(String key, String username, String mailbox) {
        try {
            String webAppTitle = activeModuleConfiguration.getChildTopics().getString(CONFIG_WEBAPP_TITLE);
            URL url = new URL(DMX_HOST_URL);
            log.info("The password reset mails token request URL should be:"
                + "\n" + url + "sign-up/password-reset/" + key);
            String href = url + "sign-up/password-reset/" + key;
            try {
                sendSystemMail(rb.getString("mail_pw_reset_title") + " " + webAppTitle,
                    rb.getString("mail_hello") + " " + getDisplayName(username) + ",<br/><br/>"+rb.getString("mail_pw_reset_body")+"<br/>"
                        + "<a href=\""+href+"\">" + href + "</a><br/><br/>" + rb.getString("mail_cheers"), mailbox);
            } catch (Exception ex) {
                log.severe("There seems to be an issue with your mail (SMTP) setup,"
                        + "we FAILED sending out the \"Password Reset\" mail, caused by: " +  ex.getMessage());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void sendNotificationMail(String username, String mailbox) {
        String webAppTitle = activeModuleConfiguration.getChildTopics().getString(CONFIG_WEBAPP_TITLE);
        //
        if (!CONFIG_ADMIN_MAILBOX.isEmpty()) {
            String adminMailbox = CONFIG_ADMIN_MAILBOX;
            try {
                sendSystemMail("Account registration on " + webAppTitle,
                        "<br/>A user has registered.<br/><br/>Username: " + username + "<br/>Email: " + mailbox, adminMailbox);
            } catch (Exception ex) {
                log.severe("There seems to be an issue with your mail (SMTP) setup,"
                        + "we FAILED notifying the \"system mailbox\" about account creation, caused by: " +  ex.getMessage());
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
    private void sendSystemMail(String subject, String message, String recipientValues) {
        String projectName = activeModuleConfiguration.getChildTopics().getString(CONFIG_PROJECT_TITLE);
        String sender = CONFIG_FROM_MAILBOX;
        String mailBody = message + "\n\n" + DMX_HOST_URL + "\n\n";
        sendmail.doEmailRecipientAs(sender, projectName, subject, mailBody, recipientValues);
    }

    private Assoc getDefaultAssociation(long topic1, long topic2) {
        return dmx.getAssocBetweenTopicAndTopic(ASSOCIATION,  topic1, topic2, DEFAULT, DEFAULT);
    }

    private Assoc getMembershipAssociation(long id, long idTwo) {
        return dmx.getAssocBetweenTopicAndTopic(MEMBERSHIP,  id, idTwo, DEFAULT, DEFAULT);
    }

    /**
     * Loads the ""org.deepamehta.sign-up.language" value from the plugin.properties file. Currently "de" and "fr"
     * are supported next to "en", which is the dfeault.
     *
     * @see init()
     */
    private void loadPluginLanguageProperty() {
        String signupPropertyLanguageValue = null;
        try {
            Properties allProperties = new Properties();
            allProperties.load(getStaticResource("/plugin.properties"));
            signupPropertyLanguageValue = allProperties.getProperty(SIGN_UP_LANGUAGE_PROPERTY);
            if (signupPropertyLanguageValue == null || signupPropertyLanguageValue.toLowerCase().equals("en")) {
                log.info("Sign-up Plugin Language option sets labels to ENGLISH");
                rb = ResourceBundle.getBundle("SignupMessages", Locale.ENGLISH);
            } else if (signupPropertyLanguageValue.toLowerCase().equals("de")) {
                log.info("Sign-up Plugin Language \"" + signupPropertyLanguageValue + "\" sets labels to GERMAN");
                rb = ResourceBundle.getBundle("SignupMessages", Locale.GERMAN);
            } else if (signupPropertyLanguageValue.toLowerCase().equals("fr")) {
                log.info("Sign-up Plugin Language \"" + signupPropertyLanguageValue + "\" sets labels to FRENCH");
                rb = ResourceBundle.getBundle("SignupMessages", Locale.FRENCH);
            }
        } catch (IOException ex) {
            log.warning("Could not find Sign-up plugin properties - use default resource bundle for labels");
            rb = ResourceBundle.getBundle("SignupMessages", Locale.ENGLISH);
        }
    }

    /**
     * The sign-up configuration object is loaded once when this bundle/plugin
     * is initialized by the framework and as soon as one configuration was
     * edited.
     *
     * @see reloadConfiguration()
     */
    private Topic getCurrentSignupConfiguration() {
        // Fixme: ### Allow for multipl sign-up configuration topics to exist and one to be active (configured).
        return dmx.getTopicByUri("dmx.signup.default_configuration");
        /** 
        Topic pluginTopic = dmx.getTopicByUri(SIGNUP_SYMOBILIC_NAME);
        return pluginTopic.getRelatedTopic(ASSOCIATION, DEFAULT, DEFAULT,
                SIGN_UP_CONFIG_TYPE_URI); **/
    }

    private Topic getCustomWorkspaceAssignmentTopic() {
        // Note: It must always be just ONE workspace related to the current module configuration
        return activeModuleConfiguration.getRelatedTopic(ASSOCIATION, DEFAULT,
                DEFAULT, WORKSPACE);
    }

    private void prepareSignupPage(String templateName) {
        if (activeModuleConfiguration != null) {
            // Notify 3rd party plugins about template preparation
            dmx.fireEvent(SIGNUP_RESOURCE_REQUESTED, context(), templateName);
            // Build up sign-up template variables
            viewData("authorization_methods", acService.getAuthorizationMethods());
            viewData("authorization_method_is_ldap", ldapAccountCreationConfigured());
            viewData("self_registration_enabled", CONFIG_SELF_REGISTRATION);
            ChildTopics configuration = activeModuleConfiguration.getChildTopics();
            viewData("title", configuration.getTopic(CONFIG_WEBAPP_TITLE).getSimpleValue().toString());
            viewData("logo_path", configuration.getTopic(CONFIG_LOGO_PATH).getSimpleValue().toString());
            viewData("css_path", configuration.getTopic(CONFIG_CSS_PATH).getSimpleValue().toString());
            viewData("project_name", configuration.getTopic(CONFIG_PROJECT_TITLE).getSimpleValue().toString());
            viewData("read_more_url", configuration.getTopic(CONFIG_READ_MORE_URL).getSimpleValue().toString());
            viewData("tos_label", configuration.getTopic(CONFIG_TOS_LABEL).getSimpleValue().toString());
            viewData("tos_details", configuration.getTopic(CONFIG_TOS_DETAILS).getSimpleValue().toString());
            viewData("pd_label", configuration.getTopic(CONFIG_PD_LABEL).getSimpleValue().toString());
            viewData("pd_details", configuration.getTopic(CONFIG_PD_DETAILS).getSimpleValue().toString());
            viewData("footer", configuration.getTopic(CONFIG_PAGES_FOOTER).getSimpleValue().toString());
            viewData("custom_workspace_enabled", configuration.getBoolean(CONFIG_API_ENABLED));
            viewData("custom_workspace_description", configuration.getTopic(CONFIG_API_DESCRIPTION).getSimpleValue().toString());
            viewData("custom_workspace_details", configuration.getTopic(CONFIG_API_DETAILS).getSimpleValue().toString());
            viewData("custom_workspace_uri", configuration.getTopic(CONFIG_API_WORKSPACE_URI).getSimpleValue().toString());
            // values used on login and registration dialogs
            viewData("start_url", configuration.getTopic(CONFIG_START_PAGE_URL).getSimpleValue().toString());
            viewData("visit_start_url", rb.getString("visit_start_url"));
            viewData("home_url", configuration.getTopic(CONFIG_HOME_PAGE_URL).getSimpleValue().toString());
            viewData("visit_home_url", rb.getString("visit_home_url"));
            viewData("loading_app_hint", configuration.getTopic(CONFIG_LOADING_HINT).getSimpleValue().toString());
            viewData("logging_out_hint", configuration.getTopic(CONFIG_LOGGING_OUT_HINT).getSimpleValue().toString());
            // messages used on login and registration dialogs
            viewData("password_length_hint", rb.getString("password_length_hint"));
            viewData("password_match_hint", rb.getString("password_match_hint"));
            viewData("check_terms_hint", rb.getString("check_terms_hint"));
            viewData("username_invalid_hint", rb.getString("username_invalid_hint"));
            viewData("username_taken_hint", rb.getString("username_taken_hint"));
            viewData("email_invalid_hint", rb.getString("email_invalid_hint"));
            viewData("email_taken_hint", rb.getString("email_taken_hint"));
            viewData("not_authorized_message", rb.getString("not_authorized_message"));
            // labels used in other templates
            viewData("signup_title", rb.getString("signup_title"));
            viewData("create_account", rb.getString("create_account"));
            viewData("login_title", rb.getString("login_title"));
            viewData("log_in_small", rb.getString("log_in_small"));
            viewData("login", rb.getString("login"));
            viewData("or_label", rb.getString("or_label"));
            viewData("logout", rb.getString("logout"));
            viewData("logged_in_as", rb.getString("logged_in_as"));
            viewData("label_username", rb.getString("label_username"));
            viewData("label_name", rb.getString("label_name"));
            viewData("label_email", rb.getString("label_email"));
            viewData("label_password", rb.getString("label_password"));
            viewData("label_password_repeat", rb.getString("label_password_repeat"));
            viewData("read_more", rb.getString("read_more"));
            viewData("label_forgot_password", rb.getString("forgot_password"));
            viewData("label_reset_password", rb.getString("reset_password"));
            viewData("info_reset_password", rb.getString("reset_password_hint"));
            viewData("password_reset_ok_message", rb.getString("password_reset_success_1"));
            //
            viewData("your_account_title", rb.getString("your_account_title"));
            viewData("your_account_heading", rb.getString("your_account_heading"));
            viewData("your_account_username_label", rb.getString("your_account_username_label"));
            viewData("your_account_email_label", rb.getString("your_account_email_label"));
            //
            viewData("api_option_title", rb.getString("api_option_title"));
            viewData("api_option_descr", rb.getString("api_option_descr"));
            viewData("api_option_revoke", rb.getString("api_option_revoke"));
            viewData("api_workspace_member", isApiWorkspaceMember());
            viewData("api_email_contact", (systemEmailContact == null) ? "" : systemEmailContact);
            viewData("api_contact_revoke", rb.getString("api_contact_revoke"));
            // complete page
            viewData("created_page_title", rb.getString("page_account_created_title"));
            viewData("created_page_body_1", rb.getString("page_account_created_body_1"));
            viewData("created_page_body_2", rb.getString("page_account_created_body_2"));
            viewData("created_page_body_3", rb.getString("page_account_created_body_3"));
            viewData("created_page_body_4", rb.getString("page_account_created_body_4"));
            // mail confirmation page
            viewData("requested_page_title", rb.getString("page_account_requested_title"));
            viewData("requested_page_1", rb.getString("page_account_requested_1"));
            viewData("requested_page_2", rb.getString("page_account_requested_2"));
            viewData("requested_page_3", rb.getString("page_account_requested_3"));
            // Generics
            String username = acService.getUsername();
            // ### viewData("administration_workspace_member", isAdministrationWorkspaceMember());
            viewData("skip_confirmation_mail_label", rb.getString("admin_skip_email_confirmation_mail"));
            viewData("administration_workspace_member", isAdministrationWorkspaceMember());
            viewData("authenticated", (username != null));
            viewData("username", username);
            viewData("template", templateName);
            viewData("hostUrl", DMX_HOST_URL);
        } else {
            log.severe("Could not load module configuration of sign-up plugin during page preparation!");
        }
    }

    private void prepareAccountEditPage() {
        String username = acService.getUsername();
        if (username != null) {
            // Make use of the new privileged getEmailAddress call for users to see their own
            String eMailAddressValue = "None";
            try {
                eMailAddressValue = dmx.getPrivilegedAccess().getEmailAddress(username);
            } catch (Exception e) {
                log.warning("Username has no Email Address topic related via \""+USER_MAILBOX_EDGE_TYPE+"\"");
            }
            viewData("logged_in", true);
            viewData("username", username);
            viewData("email", eMailAddressValue);
            viewData("link", "");
            // ### viewData("confirmed", true); // Check if user already has confirmed for a membership
        } else {
            // Not authenticated, can't do nothing but login
            viewData("logged_in", false);
            viewData("username", "Not logged in");
            viewData("email", "Not logged in");
            viewData("link", "/sign-up/login");
        }
    }

    @Override
    public void reinitTemplateEngine() {
        super.initTemplateEngine();
    }

    @Override
    public void addTemplateResolverBundle(Bundle bundle) {
        super.addTemplateResourceBundle(bundle);
    }

    @Override
    public void removeTemplateResolverBundle(Bundle bundle) {
        super.removeTemplateResourceBundle(bundle);
    }

}
