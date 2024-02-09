package systems.dmx.signup.migrations;

import systems.dmx.core.ChildTopics;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.workspaces.WorkspacesService;

import java.util.logging.Logger;

public class Migration17 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    WorkspacesService wsService;

    private static final String MIGRATION17_SIGNUP_CONFIGURATION_TYPE_URI = "dmx.signup.configuration";

    private static final String MIGRATION17_SIGNUP_DEFAULT_CONFIGURATION_URI = "dmx.signup.default_configuration";
    private static final String MIGRATION17_CONFIG_PROJECT_TITLE = "dmx.signup.config_project_title";
    private static final String MIGRATION17_CONFIG_WEBAPP_TITLE = "dmx.signup.config_webapp_title";
    private static final String MIGRATION17_CONFIG_LOGO_PATH = "dmx.signup.config_webapp_logo_path";
    private static final String MIGRATION17_CONFIG_CUSTOM_CSS_PATH = "dmx.signup.config_custom_css_path";
    private static final String MIGRATION17_CONFIG_READ_MORE_URL = "dmx.signup.config_read_more_url";
    private static final String MIGRATION17_CONFIG_PAGES_FOOTER = "dmx.signup.config_pages_footer";
    private static final String MIGRATION17_CONFIG_TOS_LABEL = "dmx.signup.config_tos_label";
    private static final String MIGRATION17_CONFIG_TOS_DETAILS = "dmx.signup.config_tos_detail";
    private static final String MIGRATION17_CONFIG_PD_LABEL = "dmx.signup.config_pd_label";
    private static final String MIGRATION17_CONFIG_PD_DETAILS = "dmx.signup.config_pd_detail";
    private static final String MIGRATION17_CONFIG_START_PAGE_URL = "dmx.signup.start_page_url";
    private static final String MIGRATION17_CONFIG_HOME_PAGE_URL = "dmx.signup.home_page_url";
    private static final String MIGRATION17_CONFIG_LOADING_APP_HINT = "dmx.signup.loading_app_hint";
    private static final String MIGRATION17_CONFIG_LOGGING_OUT_HINT = "dmx.signup.logging_out_hint";

    private static final String MIGRATION17_SIGNUP_LEGACY_CONFIGURATION_URI = "dmx.signup.legacy.configuration";


    private static final String MIGRATION17_LEGACY_SIGNUP_CONFIGURATION_TYPE_URI = "dmx.signup.legacy_configuration";
    private static final String MIGRATION17_LEGACY_CONFIG_PROJECT_TITLE = "dmx.signup.legacy_config_project_title";
    private static final String MIGRATION17_LEGACY_CONFIG_WEBAPP_TITLE = "dmx.signup.legacy_config_webapp_title";
    private static final String MIGRATION17_LEGACY_CONFIG_LOGO_PATH = "dmx.signup.legacy_config_webapp_logo_path";
    private static final String MIGRATION17_LEGACY_CONFIG_CUSTOM_CSS_PATH = "dmx.signup.legacy_config_custom_css_path";
    private static final String MIGRATION17_LEGACY_CONFIG_READ_MORE_URL = "dmx.signup.legacy_config_read_more_url";
    private static final String MIGRATION17_LEGACY_CONFIG_PAGES_FOOTER = "dmx.signup.legacy_config_pages_footer";
    private static final String MIGRATION17_LEGACY_CONFIG_TOS_LABEL = "dmx.signup.legacy_config_tos_label";
    private static final String MIGRATION17_LEGACY_CONFIG_TOS_DETAILS = "dmx.signup.legacy_config_tos_detail";
    private static final String MIGRATION17_LEGACY_CONFIG_PD_LABEL = "dmx.signup.legacy_config_pd_label";
    private static final String MIGRATION17_LEGACY_CONFIG_PD_DETAILS = "dmx.signup.legacy_config_pd_detail";

    private static final String MIGRATION17_LEGACY_CONFIG_START_PAGE_URL = "dmx.signup.legacy_config_start_page_url";

    private static final String MIGRATION17_LEGACY_CONFIG_HOME_PAGE_URL = "dmx.signup.legacy_config_home_page_url";

    private static final String MIGRATION17_LEGACY_CONFIG_LOADING_APP_HINT = "dmx.signup.legacy_config_loading_app_hint";

    private static final String MIGRATION17_LEGACY_CONFIG_LOGGING_OUT_HINT = "dmx.signup.legacy_config_logging_out_hint";


    @Override
    public void run() {
        createLegacyConfigTopicWithValuesFromDefaultConfiguration();

        assignToAdminworkspace();

        deleteDefaultCompDefsAndValues();
    }

    private void createLegacyConfigTopicWithValuesFromDefaultConfiguration() {
        Topic defaultConfiguration = dmx.getTopicByUri(MIGRATION17_SIGNUP_DEFAULT_CONFIGURATION_URI);
        ChildTopics defaultChilds = defaultConfiguration.getChildTopics();

        logger.info("###### Create legacy configuration with values coming from previous default configuration");
        ChildTopicsModel ctm = mf.newChildTopicsModel();
        ctm.set(MIGRATION17_LEGACY_CONFIG_PROJECT_TITLE, defaultChilds.getString(MIGRATION17_CONFIG_PROJECT_TITLE));
        ctm.set(MIGRATION17_LEGACY_CONFIG_WEBAPP_TITLE, defaultChilds.getString(MIGRATION17_CONFIG_WEBAPP_TITLE));
        ctm.set(MIGRATION17_LEGACY_CONFIG_LOGO_PATH, defaultChilds.getString(MIGRATION17_CONFIG_LOGO_PATH));
        ctm.set(MIGRATION17_LEGACY_CONFIG_CUSTOM_CSS_PATH, defaultChilds.getString(MIGRATION17_CONFIG_CUSTOM_CSS_PATH));
        ctm.set(MIGRATION17_LEGACY_CONFIG_READ_MORE_URL, defaultChilds.getString(MIGRATION17_CONFIG_READ_MORE_URL));
        ctm.set(MIGRATION17_LEGACY_CONFIG_PAGES_FOOTER, defaultChilds.getString(MIGRATION17_CONFIG_PAGES_FOOTER));
        ctm.set(MIGRATION17_LEGACY_CONFIG_TOS_LABEL, defaultChilds.getString(MIGRATION17_CONFIG_TOS_LABEL));
        ctm.set(MIGRATION17_LEGACY_CONFIG_TOS_DETAILS, defaultChilds.getString(MIGRATION17_CONFIG_TOS_DETAILS));
        ctm.set(MIGRATION17_LEGACY_CONFIG_PD_LABEL, defaultChilds.getString(MIGRATION17_CONFIG_PD_LABEL));
        ctm.set(MIGRATION17_LEGACY_CONFIG_PD_DETAILS, defaultChilds.getString(MIGRATION17_CONFIG_PD_DETAILS));
        ctm.set(MIGRATION17_LEGACY_CONFIG_START_PAGE_URL, defaultChilds.getString(MIGRATION17_CONFIG_START_PAGE_URL));
        ctm.set(MIGRATION17_LEGACY_CONFIG_HOME_PAGE_URL, defaultChilds.getString(MIGRATION17_CONFIG_HOME_PAGE_URL));
        ctm.set(MIGRATION17_LEGACY_CONFIG_LOADING_APP_HINT, defaultChilds.getString(MIGRATION17_CONFIG_LOADING_APP_HINT));
        ctm.set(MIGRATION17_LEGACY_CONFIG_LOGGING_OUT_HINT, defaultChilds.getString(MIGRATION17_CONFIG_LOGGING_OUT_HINT));

        dmx.createTopic(mf.newTopicModel(MIGRATION17_SIGNUP_LEGACY_CONFIGURATION_URI, MIGRATION17_LEGACY_SIGNUP_CONFIGURATION_TYPE_URI, ctm));
    }

    private void assignToAdminworkspace() {
        long administrationWsId = dmx.getPrivilegedAccess().getAdminWorkspaceId();

        logger.info("###### Migrate allSign-up legacy Configuration Topics to \"Administration\" Workspace");
        // 1 Re-Assign "Legacy Configuration" Composition Topic to "Administration"
        Topic standardConfiguration = dmx.getTopicByUri(MIGRATION17_SIGNUP_LEGACY_CONFIGURATION_URI);
        wsService.assignToWorkspace(standardConfiguration, administrationWsId);
        standardConfiguration.loadChildTopics();
        RelatedTopic webAppTitle = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_WEBAPP_TITLE);
        RelatedTopic logoPath = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_LOGO_PATH);
        RelatedTopic cssPath = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_CUSTOM_CSS_PATH);
        RelatedTopic projectTitle = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_PROJECT_TITLE);
        RelatedTopic tosLabel = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_TOS_LABEL);
        RelatedTopic tosDetail = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_TOS_DETAILS);
        RelatedTopic pdLabel = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_PD_LABEL);
        RelatedTopic pdDetail = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_PD_DETAILS);
        RelatedTopic readMoreUrl = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_READ_MORE_URL);
        RelatedTopic pagesFooter = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_PAGES_FOOTER);
        RelatedTopic startPageUrl = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_START_PAGE_URL);
        RelatedTopic homePageUrl = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_HOME_PAGE_URL);
        RelatedTopic loadingAppHint = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_LOADING_APP_HINT);
        RelatedTopic loggingOutHint = standardConfiguration.getChildTopics().getTopic(MIGRATION17_LEGACY_CONFIG_LOGGING_OUT_HINT);

        wsService.assignToWorkspace(webAppTitle, administrationWsId);
        wsService.assignToWorkspace(webAppTitle.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(logoPath, administrationWsId);
        wsService.assignToWorkspace(logoPath.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(cssPath, administrationWsId);
        wsService.assignToWorkspace(cssPath.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(projectTitle, administrationWsId);
        wsService.assignToWorkspace(projectTitle.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(tosLabel, administrationWsId);
        wsService.assignToWorkspace(tosLabel.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(tosDetail, administrationWsId);
        wsService.assignToWorkspace(tosDetail.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(pdLabel, administrationWsId);
        wsService.assignToWorkspace(pdLabel.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(pdDetail, administrationWsId);
        wsService.assignToWorkspace(pdDetail.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(readMoreUrl, administrationWsId);
        wsService.assignToWorkspace(readMoreUrl.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(pagesFooter, administrationWsId);
        wsService.assignToWorkspace(pagesFooter.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(startPageUrl, administrationWsId);
        wsService.assignToWorkspace(startPageUrl.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(homePageUrl, administrationWsId);
        wsService.assignToWorkspace(homePageUrl.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(loadingAppHint, administrationWsId);
        wsService.assignToWorkspace(loadingAppHint.getRelatingAssoc(), administrationWsId);

        wsService.assignToWorkspace(loggingOutHint, administrationWsId);
        wsService.assignToWorkspace(loggingOutHint.getRelatingAssoc(), administrationWsId);
    }

    private void deleteDefaultCompDefsAndValues() {
        TopicType signupConfigType = dmx.getTopicType(MIGRATION17_SIGNUP_CONFIGURATION_TYPE_URI);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_PROJECT_TITLE);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_WEBAPP_TITLE);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_LOGO_PATH);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_CUSTOM_CSS_PATH);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_READ_MORE_URL);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_PAGES_FOOTER);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_TOS_LABEL);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_TOS_DETAILS);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_PD_LABEL);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_PD_DETAILS);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_START_PAGE_URL);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_HOME_PAGE_URL);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_LOADING_APP_HINT);
        signupConfigType.removeCompDef(MIGRATION17_CONFIG_LOGGING_OUT_HINT);

        TopicModel defaultConfiguration = dmx.getTopicByUri(MIGRATION17_SIGNUP_DEFAULT_CONFIGURATION_URI).getModel();
        ChildTopicsModel defaultChilds = defaultConfiguration.getChildTopics();
        defaultChilds.remove(MIGRATION17_CONFIG_PROJECT_TITLE);
        defaultChilds.remove(MIGRATION17_CONFIG_WEBAPP_TITLE);
        defaultChilds.remove(MIGRATION17_CONFIG_LOGO_PATH);
        defaultChilds.remove(MIGRATION17_CONFIG_CUSTOM_CSS_PATH);
        defaultChilds.remove(MIGRATION17_CONFIG_READ_MORE_URL);
        defaultChilds.remove(MIGRATION17_CONFIG_PAGES_FOOTER);
        defaultChilds.remove(MIGRATION17_CONFIG_TOS_LABEL);
        defaultChilds.remove(MIGRATION17_CONFIG_TOS_DETAILS);
        defaultChilds.remove(MIGRATION17_CONFIG_PD_LABEL);
        defaultChilds.remove(MIGRATION17_CONFIG_PD_DETAILS);
        defaultChilds.remove(MIGRATION17_CONFIG_START_PAGE_URL);
        defaultChilds.remove(MIGRATION17_CONFIG_HOME_PAGE_URL);
        defaultChilds.remove(MIGRATION17_CONFIG_LOADING_APP_HINT);
        defaultChilds.remove(MIGRATION17_CONFIG_LOGGING_OUT_HINT);
        dmx.updateTopic(defaultConfiguration);

        purgeTopicType(MIGRATION17_CONFIG_PROJECT_TITLE);
        purgeTopicType(MIGRATION17_CONFIG_WEBAPP_TITLE);
        purgeTopicType(MIGRATION17_CONFIG_LOGO_PATH);
        purgeTopicType(MIGRATION17_CONFIG_CUSTOM_CSS_PATH);
        purgeTopicType(MIGRATION17_CONFIG_READ_MORE_URL);
        purgeTopicType(MIGRATION17_CONFIG_PAGES_FOOTER);
        purgeTopicType(MIGRATION17_CONFIG_TOS_LABEL);
        purgeTopicType(MIGRATION17_CONFIG_TOS_DETAILS);
        purgeTopicType(MIGRATION17_CONFIG_PD_LABEL);
        purgeTopicType(MIGRATION17_CONFIG_PD_DETAILS);
        purgeTopicType(MIGRATION17_CONFIG_START_PAGE_URL);
        purgeTopicType(MIGRATION17_CONFIG_HOME_PAGE_URL);
        purgeTopicType(MIGRATION17_CONFIG_LOADING_APP_HINT);
        purgeTopicType(MIGRATION17_CONFIG_LOGGING_OUT_HINT);
    }
    
    private void purgeTopicType(String s) {
        dmx.getTopicsByType(s).forEach(it -> dmx.deleteTopic(it.getId()));
        dmx.deleteTopicType(s);
    }

}