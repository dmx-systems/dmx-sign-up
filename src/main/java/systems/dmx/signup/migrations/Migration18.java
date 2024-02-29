package systems.dmx.signup.migrations;

import systems.dmx.core.service.Migration;

import java.util.logging.Logger;

public class Migration18 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    private static final String MIGRATION18_SIGNUP_CONFIGURATION_TYPE_URI = "dmx.signup.configuration";
    private static final String MIGRATION18_CONFIG_API_ENABLED = "dmx.signup.config_api_enabled";
    private static final String MIGRATION18_CONFIG_API_DESCRIPTION = "dmx.signup.config_api_description";
    private static final String MIGRATION18_CONFIG_API_DETAILS = "dmx.signup.config_api_details";
    private static final String MIGRATION18_CONFIG_API_WORKSPACE_URI = "dmx.signup.config_api_workspace_uri";

    private static final String MIGRATION18_LEGACY_SIGNUP_CONFIGURATION_TYPE_URI = "dmx.signup.legacy_configuration";
    private static final String MIGRATION18_LEGACY_CONFIG_PROJECT_TITLE = "dmx.signup.legacy_config_project_title";
    private static final String MIGRATION18_LEGACY_CONFIG_WEBAPP_TITLE = "dmx.signup.legacy_config_webapp_title";
    private static final String MIGRATION18_LEGACY_CONFIG_LOGO_PATH = "dmx.signup.legacy_config_webapp_logo_path";
    private static final String MIGRATION18_LEGACY_CONFIG_CUSTOM_CSS_PATH = "dmx.signup.legacy_config_custom_css_path";
    private static final String MIGRATION18_LEGACY_CONFIG_READ_MORE_URL = "dmx.signup.legacy_config_read_more_url";
    private static final String MIGRATION18_LEGACY_CONFIG_PAGES_FOOTER = "dmx.signup.legacy_config_pages_footer";
    private static final String MIGRATION18_LEGACY_CONFIG_TOS_LABEL = "dmx.signup.legacy_config_tos_label";
    private static final String MIGRATION18_LEGACY_CONFIG_TOS_DETAILS = "dmx.signup.legacy_config_tos_detail";
    private static final String MIGRATION18_LEGACY_CONFIG_PD_LABEL = "dmx.signup.legacy_config_pd_label";
    private static final String MIGRATION18_LEGACY_CONFIG_PD_DETAILS = "dmx.signup.legacy_config_pd_detail";

    private static final String MIGRATION18_LEGACY_CONFIG_START_PAGE_URL = "dmx.signup.legacy_config_start_page_url";

    private static final String MIGRATION18_LEGACY_CONFIG_HOME_PAGE_URL = "dmx.signup.legacy_config_home_page_url";

    private static final String MIGRATION18_LEGACY_CONFIG_LOADING_APP_HINT = "dmx.signup.legacy_config_loading_app_hint";

    private static final String MIGRATION18_LEGACY_CONFIG_LOGGING_OUT_HINT = "dmx.signup.legacy_config_logging_out_hint";

    @Override
    public void run() {
        logger.info("Removing everything related to Sign Up configuration via topics");
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_PROJECT_TITLE);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_WEBAPP_TITLE);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_LOGO_PATH);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_CUSTOM_CSS_PATH);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_READ_MORE_URL);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_PAGES_FOOTER);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_TOS_LABEL);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_TOS_DETAILS);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_PD_LABEL);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_PD_DETAILS);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_START_PAGE_URL);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_HOME_PAGE_URL);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_LOADING_APP_HINT);
        purgeTopicType(MIGRATION18_LEGACY_CONFIG_LOGGING_OUT_HINT);
        purgeTopicType(MIGRATION18_LEGACY_SIGNUP_CONFIGURATION_TYPE_URI);

        purgeTopicType(MIGRATION18_CONFIG_API_DETAILS);
        purgeTopicType(MIGRATION18_CONFIG_API_DESCRIPTION);
        purgeTopicType(MIGRATION18_CONFIG_API_ENABLED);
        purgeTopicType(MIGRATION18_CONFIG_API_WORKSPACE_URI);
        purgeTopicType(MIGRATION18_SIGNUP_CONFIGURATION_TYPE_URI);

        // Not deleting the dmx.notes.note with the URI dmx.signup.api_membership_requests here on purpose.
        // The existance of the topic in an earlier migration is being used as a marker that migration 18 and 19
        // had been run once.
    }
    
    private void purgeTopicType(String s) {
        dmx.getTopicsByType(s).forEach(it -> dmx.deleteTopic(it.getId()));
        dmx.deleteTopicType(s);
    }

}