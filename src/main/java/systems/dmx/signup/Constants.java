package systems.dmx.signup;

/**
 *
 * @author Malte Reißig
 */
public class Constants {
    
    // --- DMX Platform Type URIs --- //
    public static final String USER_MAILBOX_TYPE_URI = "dmx.contacts.email_address";
    public static final String DMX_HOST_URL = System.getProperty("dmx.host.url");
    public static final boolean DMX_ACCOUNTS_ENABLED = Boolean.parseBoolean(System.getProperty("dmx.security" +
            ".new_accounts_are_enabled"));

    // --- Sign-up config options
    public static final boolean CONFIG_SELF_REGISTRATION = Boolean.parseBoolean(System.getProperty("dmx.signup.self_registration"));
    public static final boolean CONFIG_EMAIL_CONFIRMATION = Boolean.parseBoolean(System.getProperty("dmx.signup.confirm_email_address"));
    public static final String CONFIG_ADMIN_MAILBOX = System.getProperty("dmx.signup.admin_mailbox");
    public static final String CONFIG_FROM_MAILBOX = System.getProperty("dmx.signup.system_mailbox");
    public static final boolean CONFIG_CREATE_LDAP_ACCOUNTS = Boolean.parseBoolean(System.getProperty("dmx.signup.ldap_account_creation", "false"));

    // --- Sign-up related type URIs (Configuration, Template Data) --- //
    public static final int SIGN_UP_TOKEN_TTL             = 3600000; // 60min token livetime
    public static final String SIGN_UP_CONFIG_TYPE_URI    = "dmx.signup.configuration";
    public static final String CONFIG_PROJECT_TITLE       = "dmx.signup.config_project_title";
    public static final String CONFIG_WEBAPP_TITLE        = "dmx.signup.config_webapp_title";
    public static final String CONFIG_LOGO_PATH           = "dmx.signup.config_webapp_logo_path";
    public static final String CONFIG_CSS_PATH            = "dmx.signup.config_custom_css_path";
    public static final String CONFIG_READ_MORE_URL       = "dmx.signup.config_read_more_url";
    public static final String CONFIG_PAGES_FOOTER        = "dmx.signup.config_pages_footer";
    public static final String CONFIG_TOS_LABEL           = "dmx.signup.config_tos_label";
    public static final String CONFIG_TOS_DETAILS         = "dmx.signup.config_tos_detail";
    public static final String CONFIG_PD_LABEL            = "dmx.signup.config_pd_label";
    public static final String CONFIG_PD_DETAILS          = "dmx.signup.config_pd_detail";
    public static final String CONFIG_START_PAGE_URL      = "dmx.signup.start_page_url";
    public static final String CONFIG_HOME_PAGE_URL       = "dmx.signup.home_page_url";
    public static final String CONFIG_LOADING_HINT        = "dmx.signup.loading_app_hint";
    public static final String CONFIG_LOGGING_OUT_HINT    = "dmx.signup.logging_out_hint";
    public static final String CONFIG_API_ENABLED         = "dmx.signup.config_api_enabled";
    public static final String CONFIG_API_DESCRIPTION     = "dmx.signup.config_api_description";
    public static final String CONFIG_API_DETAILS         = "dmx.signup.config_api_details";
    public static final String CONFIG_API_WORKSPACE_URI   = "dmx.signup.config_api_workspace_uri";

    public static final String SIGNUP_NAME                = "dmx.signup.name";
    public static final String SIGNUP_DISPLAY_NAME_EDGE   = "dmx.signup.display_name";
    public static final String USER_MAILBOX_EDGE_TYPE     = "dmx.base.user_mailbox";
    public static final String SIGN_UP_LANGUAGE_PROPERTY  = "dmx.signup.language";

    public static final String SIGNUP_SYMOBILIC_NAME    = "systems.dmx.sign-up";
}
