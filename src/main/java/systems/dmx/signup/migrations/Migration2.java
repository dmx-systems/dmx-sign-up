package systems.dmx.signup.migrations;

import java.util.logging.Logger;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.workspaces.WorkspacesService;

public class Migration2 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private WorkspacesService wsService;

    private static final String MIGRATION2_SIGNUP_DEFAULT_CONFIGURATION_URI = "dmx.signup.default_configuration";

    private static final String MIGRATION2_SIGNUP_CONFIGURATION = "dmx.signup.configuration";
    private static final String MIGRATION2_CONFIG_PROJECT_TITLE = "dmx.signup.config_project_title";
    private static final String MIGRATION2_CONFIG_WEBAPP_TITLE = "dmx.signup.config_webapp_title";
    private static final String MIGRATION2_CONFIG_LOGO_PATH = "dmx.signup.config_webapp_logo_path";
    private static final String MIGRATION2_CONFIG_CUSTOM_CSS_PATH = "dmx.signup.config_custom_css_path";
    private static final String MIGRATION2_CONFIG_READ_MORE_URL = "dmx.signup.config_read_more_url";
    private static final String MIGRATION2_CONFIG_PAGES_FOOTER = "dmx.signup.config_pages_footer";
    private static final String MIGRATION2_CONFIG_TOS_LABEL = "dmx.signup.config_tos_label";
    private static final String MIGRATION2_CONFIG_TOS_DETAILS = "dmx.signup.config_tos_detail";
    private static final String MIGRATION2_CONFIG_PD_LABEL = "dmx.signup.config_pd_label";
    private static final String MIGRATION2_CONFIG_PD_DETAILS = "dmx.signup.config_pd_detail";

    private static final String DEFAULT_CONFIG_PROJECT_TITLE = System.getProperty(MIGRATION2_CONFIG_PROJECT_TITLE, "My DMX");
    private static final String DEFAULT_CONFIG_WEBAPP_TITLE = System.getProperty(MIGRATION2_CONFIG_WEBAPP_TITLE, "My DMX");
    private static final String DEFAULT_CONFIG_LOGO_PATH = System.getProperty(MIGRATION2_CONFIG_LOGO_PATH, "/systems.dmx.sign-up/dmx-logo.svg");
    private static final String DEFAULT_CONFIG_CUSTOM_CSS_PATH = System.getProperty(MIGRATION2_CONFIG_CUSTOM_CSS_PATH, "/systems.dmx.sign-up/style/style.css");
    private static final String DEFAULT_CONFIG_READ_MORE_URL = System.getProperty(MIGRATION2_CONFIG_READ_MORE_URL, "...");
    private static final String DEFAULT_CONFIG_PAGES_FOOTER = System.getProperty(MIGRATION2_CONFIG_PAGES_FOOTER, "standard");
    private static final String DEFAULT_CONFIG_TOS_LABEL = System.getProperty(MIGRATION2_CONFIG_TOS_LABEL, "Terms of Service");
    private static final String DEFAULT_CONFIG_TOS_DETAILS = System.getProperty(MIGRATION2_CONFIG_TOS_DETAILS, "...");
    private static final String DEFAULT_CONFIG_PD_LABEL = System.getProperty(MIGRATION2_CONFIG_PD_LABEL, "I understand that any private information I give to this site may be made publicly available.");
    private static final String DEFAULT_CONFIG_PD_DETAILS = System.getProperty(MIGRATION2_CONFIG_PD_DETAILS, "...");

    @Override
    public void run() {
        // Creates default configuration with default values coming from platform configuration or built-in values.
        ChildTopicsModel ctm = mf.newChildTopicsModel();
        ctm.set(MIGRATION2_CONFIG_PROJECT_TITLE, DEFAULT_CONFIG_PROJECT_TITLE);
        ctm.set(MIGRATION2_CONFIG_WEBAPP_TITLE, DEFAULT_CONFIG_WEBAPP_TITLE);
        ctm.set(MIGRATION2_CONFIG_LOGO_PATH, DEFAULT_CONFIG_LOGO_PATH);
        ctm.set(MIGRATION2_CONFIG_CUSTOM_CSS_PATH, DEFAULT_CONFIG_CUSTOM_CSS_PATH);
        ctm.set(MIGRATION2_CONFIG_READ_MORE_URL, DEFAULT_CONFIG_READ_MORE_URL);
        ctm.set(MIGRATION2_CONFIG_PAGES_FOOTER, DEFAULT_CONFIG_PAGES_FOOTER);
        ctm.set(MIGRATION2_CONFIG_TOS_LABEL, DEFAULT_CONFIG_TOS_LABEL);
        ctm.set(MIGRATION2_CONFIG_TOS_DETAILS, DEFAULT_CONFIG_TOS_DETAILS);
        ctm.set(MIGRATION2_CONFIG_PD_LABEL, DEFAULT_CONFIG_PD_LABEL);
        ctm.set(MIGRATION2_CONFIG_PD_DETAILS, DEFAULT_CONFIG_PD_DETAILS);

        dmx.createTopic(
                mf.newTopicModel(
                        MIGRATION2_SIGNUP_DEFAULT_CONFIGURATION_URI,
                        MIGRATION2_SIGNUP_CONFIGURATION,
                        ctm)
        );

        /** Topic plugin = dmx.getTopicByUri(SignupPlugin.SIGNUP_SYMOBILIC_NAME);
        Topic standardConfiguration = dmx.getTopicByUri("dmx.signup.default_configuration");
        // 1) Assign the (default) "Sign-up Configuration" to the Plugin topic
        logger.info("Sign-up => Assigning default \"Sign-up Configuration\" to \"DMX Sign up\" Topic");
        Assoc assoc = dmx.createAssoc(mf.newAssocModel("dmx.core.association",
                mf.newTopicPlayerModel(plugin.getId(), "dmx.core.default"),
                mf.newTopicPlayerModel(standardConfiguration.getId(), "dmx.core.default")
        ));
        // 2) Set Configuration Topic Workspace Assignment to ("System") (editable for admin)
        Topic systemWorkspace = wsService.getWorkspace(AccessControlService.SYSTEM_WORKSPACE_URI);
        wsService.assignToWorkspace(standardConfiguration, systemWorkspace.getId());
        wsService.assignToWorkspace(assoc, systemWorkspace.getId()); **/

    }

}