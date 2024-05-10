package systems.dmx.signup;

import io.swagger.v3.oas.annotations.Hidden;
import org.apache.commons.lang3.StringUtils;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.accesscontrol.event.PostLoginUser;
import systems.dmx.accountmanagement.AccountManagementService;
import systems.dmx.core.Assoc;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.EventListener;
import systems.dmx.core.service.*;
import systems.dmx.core.service.accesscontrol.Credentials;
import systems.dmx.core.service.event.AllPluginsActive;
import systems.dmx.core.service.event.PostUpdateTopic;
import systems.dmx.core.storage.spi.DMXTransaction;
import systems.dmx.facets.FacetsService;
import systems.dmx.sendmail.SendmailService;
import systems.dmx.signup.configuration.AccountCreation;
import systems.dmx.signup.configuration.Configuration;
import systems.dmx.signup.di.DaggerSignupComponent;
import systems.dmx.signup.di.SignupComponent;
import systems.dmx.signup.mapper.IsValidEmailAdressMapper;
import systems.dmx.signup.mapper.NewAccountDataMapper;
import systems.dmx.signup.model.NewAccountData;
import systems.dmx.signup.model.NewAccountTokenData;
import systems.dmx.signup.model.PasswordResetTokenData;
import systems.dmx.signup.usecase.*;
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

import static systems.dmx.accesscontrol.Constants.LOGIN_ENABLED;
import static systems.dmx.accesscontrol.Constants.USERNAME;
import static systems.dmx.core.Constants.CHILD;
import static systems.dmx.core.Constants.PARENT;
import static systems.dmx.signup.Constants.*;
import static systems.dmx.signup.configuration.SignUpConfigOptions.*;

/**
 * This plugin enables anonymous users to create themselves a user account in DMX through an (optional) Email based
 * confirmation workflow and thus it depends on the dmx-sendmail plugin and e.g. postfix like "internet" installation
 * for "localhost". Source code available at: https://git.dmx.systems/dmx-plugins/dmx-sign-up
 *
 * @author Malte Rei&szlig;ig et al
 * @version 2.0.0-SNAPSHOT
 **/
@Path("/sign-up")
@Produces(MediaType.APPLICATION_JSON)
public class SignupPlugin extends PluginActivator implements SignupService, PostUpdateTopic, PostLoginUser, AllPluginsActive {

    static final Logger logger = Logger.getLogger(SignupPlugin.class.getName());

    @Inject
    AccessControlService accesscontrol;
    @Inject
    private FacetsService facets;
    @Inject
    private SendmailService sendmail;
    @Inject
    private WorkspacesService workspacesService;

    @Inject
    private AccountManagementService accountManagementService;

    @Context
    UriInfo uri;

    HashMap<String, NewAccountTokenData> newAccountTokenData = new HashMap<>();
    HashMap<String, PasswordResetTokenData> passwordResetTokenData = new HashMap<>();

    HashMap<String, String> deferredDisplayName = new HashMap<>();

    EmailTextProducer emailTextProducer = new InternalEmailTextProducer();

    NewAccountDataMapper newAccountDataMapper;

    IsValidEmailAdressMapper isValidEmailAdressMapper;

    GetAccountCreationPasswordUseCase getAccountCreationPasswordUseCase;

    HasAccountCreationPrivilegeUseCase hasAccountCreationPrivilegeUseCase;

    IsPasswordComplexEnoughUseCase isPasswordComplexEnoughUseCase;

    LogAndVerifyConfigurationUseCase logAndVerifyConfigurationUseCase;

    SendMailUseCase sendMailUseCase;

    private Configuration configuration;

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    // --- Hooks --- //
    private void runDependencyInjection() {
        // DI:
        SignupComponent component = DaggerSignupComponent.builder()
                .coreService(dmx)
                .accessControlService(accesscontrol)
                .workspacesService(workspacesService)
                .sendmailService(sendmail)
                .build();

        newAccountDataMapper = component.newAccountDataMapper();
        isValidEmailAdressMapper = component.isValidEmailAdressMapper();
        getAccountCreationPasswordUseCase = component.getAccountCreationPasswordUseCase();
        hasAccountCreationPrivilegeUseCase = component.hasAccountCreationPrivilegeUseCase();
        isPasswordComplexEnoughUseCase = component.isPasswordComplexEnoughUseCase();
        logAndVerifyConfigurationUseCase = component.logAndVerifyConfigurationUseCase();
        sendMailUseCase = component.sendMailUseCase();
    }

    @Override
    public void init() {
        runDependencyInjection();
    }

    @Override
    public void allPluginsActive() {
        // Log configuration settings
        configuration = logAndVerifyConfigurationUseCase.invoke(getAuthorizationMethods());
    }

    // --- SignupService Implementation --- //

    @Override
    public void setEmailTextProducer(EmailTextProducer emailTextProducer) {
        if (emailTextProducer == null) {
            throw new IllegalArgumentException("New instance cannot be null");
        }
        this.emailTextProducer = emailTextProducer;
    }

    /**
     * Custom event fired by sign-up module up on successful user account creation.
     *
     * @return Topic    The username topic (related to newly created user account
     * topic).
     */
    static DMXEvent USER_ACCOUNT_CREATE_LISTENER = new DMXEvent(UserAccountCreateListener.class) {
        @Override
        public void dispatch(EventListener listener, Object... params) {
            ((UserAccountCreateListener) listener).userAccountCreated((Topic) params[0]);
        }
    };

    @GET
    @Path("/display-name/{username}")
    @Override
    public String getDisplayName(@PathParam("username") String username) {
        try {
            Topic usernameTopic = dmx.getPrivilegedAccess().getUsernameTopic(username);
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
                Topic usernameTopic = dmx.getPrivilegedAccess().getUsernameTopic(username);
                if (usernameTopic != null) {
                    facets.updateFacet(usernameTopic, DISPLAY_NAME_FACET,
                            mf.newFacetValueModel(DISPLAY_NAME).set(displayName)
                    );
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Updating display name of user '" + username + "' failed, displayName='" +
                    displayName + "'", e);
        }
    }

    private NewAccountData mapToNewAccountData(String methodName, String username, String emailAddress, String displayName) {
        return newAccountDataMapper.map(CONFIG_USERNAME_POLICY, methodName, username, emailAddress, displayName);
    }

    @Override
    public SignUpRequestResult requestSignUp(String methodName, String username, String emailAddress, String displayName, String providedPassword,
                                             boolean skipConfirmation) {
        if (!isSelfRegistrationEnabled() && !hasAccountCreationPrivilege()) {
            return new SignUpRequestResult(SignUpRequestResult.Code.ACCOUNT_CREATION_DENIED);
        }
        if (!isValidEmailAdressMapper.map(emailAddress)) {
            return new SignUpRequestResult(SignUpRequestResult.Code.ERROR_INVALID_EMAIL);
        }
        NewAccountData newAccountData = mapToNewAccountData(methodName, username, emailAddress, displayName);
        String password = getAccountCreationPasswordUseCase.invoke(configuration.getAccountCreationPasswordHandling(), providedPassword);
        if (Objects.equals(password, providedPassword) && !isPasswordComplexEnough(password)) {
            return new SignUpRequestResult(SignUpRequestResult.Code.ERROR_PASSWORD_COMPLEXITY_INSUFFICIENT);
        }
        try {
            if (configuration.isEmailConfirmationEnabled()) {
                return handleSignUpWithEmailConfirmation(newAccountData, password, skipConfirmation);
            } else {
                return handleSignUpWithDirectAccountCreation(newAccountData, password);
            }
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Could not build response URI while handling sign-up request", e);
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
            if (configuration.getAccountCreation() == AccountCreation.ADMIN) {
                logger.info("Sign-up Configuration: Email based confirmation workflow active but admin is " +
                        "skipping confirmation mail.");
                try {
                    transactional(() -> createCustomUserAccount(newAccountData, password));
                } catch (Exception e) {
                    return new SignUpRequestResult(SignUpRequestResult.Code.UNEXPECTED_ERROR);
                }
                return handleAccountCreatedRedirect(newAccountData.username);
            } else {
                logger.warning("Non-privileged user attempted to skip confirmation email. Username: " + accesscontrol.getUsername());
                // skipping confirmation is only allowed for admins
                return new SignUpRequestResult(SignUpRequestResult.Code.ADMIN_PRIVILEGE_MISSING);
            }
        } else {
            logger.fine("Sign-up Configuration: Email based confirmation workflow active. Sending out confirmation mail.");
            String tokenKey = createUserValidationToken(newAccountData, password);
            sendConfirmationMail(tokenKey, newAccountData.displayName, newAccountData.emailAddress);
            // redirect user to a "token-info" page
            return new SignUpRequestResult(SignUpRequestResult.Code.SUCCESS_EMAIL_CONFIRMATION_NEEDED);
        }
    }

    private SignUpRequestResult handleAccountCreatedRedirect(String username) {
        if (DMX_ACCOUNTS_ENABLED) {
            logger.info("DMX Config: The new account is now ENABLED.");
            return new SignUpRequestResult(SignUpRequestResult.Code.SUCCESS_ACCOUNT_CREATED, username);
        } else {
            logger.info("DMX Config: The new account is now DISABLED.");
            return new SignUpRequestResult(SignUpRequestResult.Code.SUCCESS_ACCOUNT_PENDING, username);
        }
    }

    @Override
    public ProcessSignUpRequestResult requestProcessSignUp(String token) {
        // 1) Check if token exists: It may not exist due to eg. bundle refresh, system restart, token invalid
        if (!newAccountTokenData.containsKey(token)) {
            return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.INVALID_TOKEN);
        }
        // 2) Process available token and remove it from stack
        NewAccountTokenData tokenData = newAccountTokenData.remove(token);
        // 3) Create the user account and show ok OR present an error message.
        try {
            if (tokenData.expiration.isAfter(Instant.now())) {
                logger.log(Level.INFO, "Trying to create user account for {0}", tokenData.accountData.emailAddress);
                try {
                    transactional(() -> createCustomUserAccount(tokenData.accountData, tokenData.password));
                } catch (Exception e) {
                    return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.UNEXPECTED_ERROR);
                }
            } else {
                return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.LINK_EXPIRED);
            }
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Account creation failed", ex);
            return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.UNEXPECTED_ERROR);
        }
        logger.log(Level.INFO, "Account successfully created for username: {0}", tokenData.accountData.username);
        if (!DMX_ACCOUNTS_ENABLED) {
            logger.info("Account activation by an administrator remains PENDING ");
            return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.SUCCESS_ACCOUNT_PENDING);
        }
        return new ProcessSignUpRequestResult(ProcessSignUpRequestResult.Code.SUCCESS, tokenData.accountData.username);
    }

    @Override
    public InitiatePasswordResetRequestResult requestInitiateRedirectPasswordReset(String emailAddress, String redirectUrl) {
        logger.info("Password reset requested for user with email address: '" + emailAddress + "' wishing to redirect to: '" +
                redirectUrl + "'");
        if (dmx.getPrivilegedAccess().emailAddressExists(emailAddress)) {
            logger.info("Email based password reset workflow do'able, sending out passwort reset mail.");
            // ### Todo: Add/include return Url to token (!)
            // Note: Here system can't know "display name" (anonymous has
            // no read permission on it) and thus can't pass it on
            // TODO: use privileged API to get display name
            sendPasswordResetToken(emailAddress, null, redirectUrl);
            return InitiatePasswordResetRequestResult.SUCCESS;
        }
        logger.warning("Email based password reset workflow not possible because email address is not known: " + emailAddress);
        return InitiatePasswordResetRequestResult.EMAIL_UNKNOWN;
    }

    // Note: called by anonymous (a user forgot his password), so it must be @GET.
    // For anonymous @POST/@PUT would be rejected by DMX platform's request filter.
    @GET
    @Path("/password-reset/{emailAddress}")
    @Override
    public InitiatePasswordResetRequestResult requestInitiatePasswordReset(@PathParam("emailAddress") String emailAddress,
                                                                           @QueryParam("name") String displayName) {
        logger.info("Password reset requested for user with Email: '" + emailAddress + "' and display name: '" + displayName + "'");
        try {
            if (!isValidEmailAdressMapper.map(emailAddress)) {
                return InitiatePasswordResetRequestResult.UNEXPECTED_ERROR;
            }
            if (dmx.getPrivilegedAccess().emailAddressExists(emailAddress)) {
                logger.info("Email based password reset workflow possible, sending out passwort reset mail.");
                sendPasswordResetToken(emailAddress, displayName, null);
                return InitiatePasswordResetRequestResult.SUCCESS;
            } else {
                logger.info("Email based password reset workflow not possible because mail address not known: " +
                        emailAddress);
                return InitiatePasswordResetRequestResult.EMAIL_UNKNOWN;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return InitiatePasswordResetRequestResult.UNEXPECTED_ERROR;
    }

    @GET
    @Path("/token/{token}")
    @Override
    public PasswordResetTokenCheckRequestResult requestPasswordResetTokenCheck(@PathParam("token") String token) {
        // TODO: Add some general token validation

        // 1) Assert token exists: It may not exist due to e.g. bundle refresh, system restart, token invalid
        if (!passwordResetTokenData.containsKey(token)) {
            return new PasswordResetTokenCheckRequestResult(PasswordResetTokenCheckRequestResult.Code.INVALID_TOKEN);
        }
        // 2) Attempt to get token data
        PasswordResetTokenData tokenData = passwordResetTokenData.get(token);
        // 3) Check token data existance and validity
        if (tokenData != null && tokenData.expiration.isAfter(Instant.now())) {
            // token data valid (and so it token)
            return new PasswordResetTokenCheckRequestResult(PasswordResetTokenCheckRequestResult.Code.SUCCESS, tokenData.accountData.username,
                    tokenData.accountData.emailAddress, tokenData.accountData.displayName, tokenData.redirectUrl);
        } else {
            // missing token data (token already used or token invalid) or expired validity
            logger.warning("The provided password reset token '" + token + "' has expired or is invalid");
            passwordResetTokenData.remove(token);
            return new PasswordResetTokenCheckRequestResult(PasswordResetTokenCheckRequestResult.Code.LINK_EXPIRED);
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
    @Path("/password-reset/{token}/{password}")
    @Transactional
    @Override
    public PasswordChangeRequestResult requestPasswordChange(@PathParam("token") String token,
                                                             @PathParam("password") String password) {
        logger.info("Processing Password Update Request Token... ");
        PasswordResetTokenData tokenData = passwordResetTokenData.get(token);
        if (tokenData != null) {
            if (!isPasswordComplexEnough(password)) {
                return PasswordChangeRequestResult.PASSWORD_COMPLEXITY_INSUFFICIENT;
            }
            Credentials newCreds = new Credentials(tokenData.accountData.username, password);
            try {
                // TODO: Must change dmx-signup password change API to provide current credentials properly
                accountManagementService.changePassword(null, newCreds);
            } catch (Exception e) {
                logger.severe("Credentials for user " + newCreds.username + " COULD NOT be changed succesfully.");
                return PasswordChangeRequestResult.PASSWORD_CHANGE_FAILED;
            }
            passwordResetTokenData.remove(token);
            return PasswordChangeRequestResult.SUCCESS;
        } else {
            return PasswordChangeRequestResult.NO_TOKEN;
        }
    }

    @POST
    @Path("/user-account/{username}/{emailAddress}/{displayname}/{password}")
    @Transactional
    @Override
    @Hidden
    public Topic createUserAccount(@PathParam("username") String username,
                                   @PathParam("emailAddress") String emailAddress,
                                   @PathParam("displayname") String displayName,
                                   @PathParam("password") String password) {
        logger.info("Creating user account with display name \"" + displayName + "\" and email address \"" + emailAddress +
                "\"");
        hasAccountCreationPrivilegeUseCase.checkAccountCreation();
        Topic usernameTopic = createCustomUserAccount(mapToNewAccountData(null, username, emailAddress, displayName), password);
        return usernameTopic;
    }

    private void setupDisplayName(String username, String displayName) throws Exception {
        // 2) create and assign displayname topic to "System" workspace
        final Topic usernameTopic = dmx.getPrivilegedAccess().getUsernameTopic(username);
        final long usernameTopicId = usernameTopic.getId();
        long displayNamesWorkspaceId = getDisplayNamesWorkspaceId();

        dmx.getPrivilegedAccess().runInWorkspaceContext(-1, new Callable<Topic>() {
            @Override
            public Topic call() {
                // create display name facet for username topic
                facets.addFacetTypeToTopic(usernameTopicId, DISPLAY_NAME_FACET);
                facets.updateFacet(usernameTopicId, DISPLAY_NAME_FACET, mf.newFacetValueModel(DISPLAY_NAME)
                        .set(displayName));
                // automatically make users member in "Display Names" workspace
                dmx.getPrivilegedAccess().createMembership(username, displayNamesWorkspaceId);
                logger.info("Created membership for new user account in \"Display Names\" workspace " +
                        "(SharingMode.Collaborative)");
                // Account creator should be member of "Display Names"
                RelatedTopic result = facets.getFacet(usernameTopicId, DISPLAY_NAME_FACET);
                dmx.getPrivilegedAccess().assignToWorkspace(result, displayNamesWorkspaceId);

                return result;
            }
        });
    }

    private Topic createCustomUserAccount(NewAccountData newAccountData, String password) {
        try {
            // 1) NewAccountData is set according to username policy
            String username = createSimpleUserAccount(newAccountData.methodName, newAccountData.username, password,
                    newAccountData.emailAddress);
            final String displayName = newAccountData.displayName;

            if (hasAccountCreationPrivilege()) {
                setupDisplayName(username, displayName);
            } else {
                // Diplayname needs to be set up on first login
                deferredDisplayName.put(username, displayName);
            }

            return dmx.getPrivilegedAccess().getUsernameTopic(username);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to create custom account", e);
            throw new RuntimeException("Creating custom user account failed, mailbox='" + newAccountData.emailAddress +
                    "', displayName='" + newAccountData.displayName + "'", e);
        }
    }

    public long getDisplayNamesWorkspaceId() {
        Topic ws = workspacesService.getWorkspace(DISPLAY_NAME_WS_URI);
        return (ws != null) ? ws.getId() : -1;
    }

    // --- Listeners --- //

    @Override
    public void postUpdateTopic(Topic topic, ChangeReport report, TopicModel updateModel) {
        if (topic.getTypeUri().equals(LOGIN_ENABLED)) {
            // Account status
            boolean status = Boolean.parseBoolean(topic.getSimpleValue().toString());
            // Account involved
            Topic username = topic.getRelatedTopic("dmx.config.configuration", null, null, USERNAME);
            // Perform notification
            if (status && !DMX_ACCOUNTS_ENABLED) { // Enabled=true && new_accounts_are_enabled=false
                logger.info("Sign-up Notification: User Account \"" + username.getSimpleValue() + "\" is now ENABLED!");
                Topic mailbox = username.getRelatedTopic(USER_MAILBOX_EDGE_TYPE, null, null, USER_MAILBOX_TYPE_URI);
                if (mailbox != null) { // for accounts created via sign-up plugin this will always evaluate to true
                    String mailboxValue = mailbox.getSimpleValue().toString();
                    String subject = emailTextProducer.getAccountActiveEmailSubject();
                    String message = emailTextProducer.getAccountActiveEmailMessage(username.toString());
                    sendMail(subject, message, mailboxValue);
                    logger.info("Send system notification mail to " + mailboxValue + " - The account is now active!");
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
        if (StringUtils.isNotBlank(configuration.getAdminEmailAddress())) {
            String recipient = configuration.getAdminEmailAddress();
            try {
                sendMail(subject, message, recipient);
            } catch (Exception ex) {
                throw new RuntimeException("There seems to be an issue with your mail (SMTP) setup, we FAILED sending out a " +
                        "notification mail to the admin", ex);
            }
        } else {
            logger.warning("Did not send notification mail to System Mailbox - Admin Mailbox configuration not set");
        }
    }

    @Override
    public boolean isAccountCreationPasswordEditable() {
        return configuration.getAccountCreationPasswordHandling() == AccountCreation.PasswordHandling.EDITABLE;
    }

    private Topic createUsername(Credentials credentials) throws Exception {
        return accountManagementService._createUserAccount(credentials);
    }

    private String createSimpleUserAccount(String methodName, String username, String password, String emailAddress) {
        try {
            if (isUsernameTaken(username)) {
                // Might be thrown if two users compete for registration (of the same username)
                // within the same 60 minutes (tokens validity timespan). First confirming, wins.
                throw new RuntimeException("Username '" + username + "' was already registered and confirmed");
            }

            // 1) Creates a new username topic (in LDAP and/or DMX)
            Credentials creds = new Credentials(username, password);
            creds.methodName = methodName;
            final Topic usernameTopic = createUsername(creds);

            if (usernameTopic == null) {
                throw new RuntimeException("Username topic had not been created. Cannot continue.");
            }

            dmx.getPrivilegedAccess().runInWorkspaceContext(-1, new Callable<Topic>() {
                @Override
                public Topic call() {
                    // 2) create and associate e-mail address topic in "System" Workspace
                    long systemWorkspaceId = dmx.getPrivilegedAccess().getSystemWorkspaceId();
                    Topic emailAddressTopic = dmx.createTopic(mf.newTopicModel(USER_MAILBOX_TYPE_URI,
                            new SimpleValue(emailAddress)));
                    dmx.getPrivilegedAccess().assignToWorkspace(emailAddressTopic, systemWorkspaceId);
                    // 3) fire custom event ### this is useless since fired by "anonymous" (this request scope)
                    dmx.fireEvent(USER_ACCOUNT_CREATE_LISTENER, usernameTopic);
                    // 4) associate email address to "username" topic too
                    Assoc assoc = dmx.createAssoc(mf.newAssocModel(USER_MAILBOX_EDGE_TYPE,
                            mf.newTopicPlayerModel(emailAddressTopic.getId(), CHILD),
                            mf.newTopicPlayerModel(usernameTopic.getId(), PARENT)));
                    dmx.getPrivilegedAccess().assignToWorkspace(assoc, systemWorkspaceId);
                    return emailAddressTopic;
                }
            });
            logger.info("Created new user account for user '" + username + "' and " + emailAddress);
            // 6) Inform administrations about successfull account creation
            sendNotificationMail(username, emailAddress);
            return username;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Creating simple user account failed", e);
            throw new RuntimeException("Creating simple user account failed, username='" + username +
                    "', mailbox='" + emailAddress + "'", e);
        }
    }

    @GET
    @Path("/email/{email}/taken")
    @Override
    public boolean isEmailAddressTaken(@PathParam("email") String email) {
        return dmx.getPrivilegedAccess().emailAddressExists(email.toLowerCase().trim());
    }

    @GET
    @Path("/username/{username}/taken")
    @Override
    public boolean isUsernameTaken(@PathParam("username") String username) {
        return dmx.getPrivilegedAccess().getUsernameTopic(username.trim()) != null;
    }

    @Override
    public Boolean isPasswordComplexEnough(String password) {
        return isPasswordComplexEnoughUseCase.invoke(configuration.getExpectedPasswordComplexity(), password);
    }

    // --- Private Helpers --- //

    @Override
    public boolean isSelfRegistrationEnabled() {
        return configuration.getAccountCreation() == AccountCreation.PUBLIC;
    }

    @Override
    public boolean hasAccountCreationPrivilege() {
        return hasAccountCreationPrivilegeUseCase.invoke();
    }

    private void sendPasswordResetToken(String emailAddress, String displayName, String redirectUrl) {
        String username = dmx.getPrivilegedAccess().getUsername(emailAddress);
        // Todo: Would need privileged access to "Display Name" to display it in password-update dialog
        String tokenKey = createPasswordResetTokenData(username, emailAddress, displayName, redirectUrl);
        sendPasswordResetMail(tokenKey, username, emailAddress.trim(), displayName);
    }

    private String createUserValidationToken(NewAccountData newAccountData, String password) {
        String tokenKey = UUID.randomUUID().toString();
        Instant expiration = calculateTokenExpiration();
        NewAccountTokenData token = new NewAccountTokenData(newAccountData, password, expiration);
        newAccountTokenData.put(tokenKey, token);
        logger.log(Level.INFO, "Set up key {0} for {1} sending confirmation mail valid till {3}",
                new Object[]{tokenKey, newAccountData.emailAddress, expiration});
        return tokenKey;
    }

    private Instant calculateTokenExpiration() {
        return Instant.now().plus(configuration.getTokenExpirationDuration());
    }

    private String createPasswordResetTokenData(String username, String emailAddress, String displayName, String redirectUrl) {
        String token = UUID.randomUUID().toString();
        Instant expiration = calculateTokenExpiration();
        PasswordResetTokenData tokenData = new PasswordResetTokenData(
                new NewAccountData(null, username, emailAddress, displayName),
                expiration,
                redirectUrl
        );
        passwordResetTokenData.put(token, tokenData);
        logger.log(Level.INFO, "Set up password reset token data with token {0} for email address {1} valid until {3}",
                new Object[]{token, emailAddress, expiration});
        return token;
    }

    private void sendConfirmationMail(String key, String username, String emailAddress) {
        try {
            if (DMX_ACCOUNTS_ENABLED) {
                String mailSubject = emailTextProducer.getConfirmationActiveMailSubject();
                String message = emailTextProducer.getConfirmationActiveMailMessage(username, key);
                sendMail(mailSubject, message, emailAddress);
            } else {
                String mailSubject = emailTextProducer.getConfirmationProceedMailSubject();
                String message = emailTextProducer.getUserConfirmationProceedMailMessage(username, key);
                sendMail(mailSubject, message, emailAddress);
            }
        } catch (RuntimeException ex) {
            throw new RuntimeException(
                    "There seems to be an issue with your mail (SMTP) setup, we FAILED sending out the 'confirmation mail'", ex);
        }
    }

    private void sendPasswordResetMail(String key, String username, String emailAddress, String displayName) {
        try {
            String addressee = (displayName != null && !displayName.isEmpty()) ? displayName : username;
            String subject = emailTextProducer.getPasswordResetMailSubject();
            String message = emailTextProducer.getPasswordResetMailMessage(addressee, key);
            sendMail(subject, message, emailAddress);
        } catch (RuntimeException ex) {
            throw new RuntimeException(
                    "There seems to be an issue with your mail (SMTP) setup, we FAILED sending out the 'password reset' mail", ex);
        }
    }

    /**
     * Sends an email to the admin (as configured by "dmx.signup.admin_mailbox") about successful account creation.
     */
    private void sendNotificationMail(String username, String emailAddress) {
        try {
            String subject = emailTextProducer.getAccountCreationSystemEmailSubject();
            String message = emailTextProducer.getAccountCreationSystemEmailMessage(username, emailAddress);
            sendMail(subject, message, configuration.getAdminEmailAddress());
        } catch (Exception ex) {
            throw new RuntimeException(
                    "There seems to be an issue with your mail (SMTP) setup, we FAILED notifying the 'system mailbox' about account creation", ex);
        }
    }

    /**
     * @param subject         String Subject text for the message.
     * @param message         String Text content of the message.
     * @param recipientValues String of Email Address message is sent to **must not** be NULL.
     */
    private void sendMail(String subject, String message, String recipientValues) {
        String senderName = configuration.getFromName();
        String sender = configuration.getFromEmailAddress();
        boolean isHtml = emailTextProducer.isHtml();
        sendMailUseCase.invoke(sender, senderName, subject, message, recipientValues, isHtml);
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
        if (!CONFIG_RESTRICT_AUTH_METHODS.trim().isEmpty()) {
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
            logger.warning("A custom transaction failed: " + t.getLocalizedMessage());
            tx.failure();
            throw t;
        } finally {
            tx.finish();
        }
    }

    @Override
    public void postLoginUser(String loggedInUserName) {
        String displayName = deferredDisplayName.get(loggedInUserName);
        if (accesscontrol.getUsername().equals(loggedInUserName)
                && displayName != null) {
            logger.info("Handling deferred display name for user " + loggedInUserName);
            transactional(() -> {
                try {
                    setupDisplayName(loggedInUserName, displayName);
                    deferredDisplayName.remove(loggedInUserName);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to set up the deferred username for " + loggedInUserName, e);
                }
            });
        }
    }

}
