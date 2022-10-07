package systems.dmx.signup.configuration;

import systems.dmx.core.Topic;

import static systems.dmx.core.Constants.ASSOCIATION;
import static systems.dmx.core.Constants.DEFAULT;
import static systems.dmx.signup.Constants.*;
import static systems.dmx.workspaces.Constants.WORKSPACE;

public class ModuleConfiguration {

    private Topic topic;

    public ModuleConfiguration(Topic topic) {
        this.topic = topic;
    }

    public String getConfigurationUri() {
        return topic.getUri();
    }

    public String getConfigurationName() {
        return topic.getSimpleValue().toString();
    }

    public boolean isValid() {
        return topic != null;
    }

    public void reload() {
        topic.loadChildTopics();
    }

    public String getProjectTitle() {
        return topic.getChildTopics().getString(CONFIG_PROJECT_TITLE);
    }

    public String getWebAppTitle() {
        return topic.getChildTopics().getTopic(CONFIG_WEBAPP_TITLE).getSimpleValue().toString();
    }

    public String getLogoPath() {
        return topic.getChildTopics().getTopic(CONFIG_LOGO_PATH).getSimpleValue().toString();
    }

    public String getCssPath() {
        return topic.getChildTopics().getTopic(CONFIG_CSS_PATH).getSimpleValue().toString();
    }

    public String getReadMoreUrl() {
        return topic.getChildTopics().getTopic(CONFIG_READ_MORE_URL).getSimpleValue().toString();
    }
    public String getTosLabel() {
        return topic.getChildTopics().getTopic(CONFIG_TOS_LABEL).getSimpleValue().toString();
    }

    public String getTosDetails() {
        return topic.getChildTopics().getTopic(CONFIG_TOS_DETAILS).getSimpleValue().toString();
    }

    public String getPdLabel() {
        return topic.getChildTopics().getTopic(CONFIG_PD_LABEL).getSimpleValue().toString();
    }

    public String getPdDetails() {
        return topic.getChildTopics().getTopic(CONFIG_PD_DETAILS).getSimpleValue().toString();
    }

    public String getPagesFooter() {
        return topic.getChildTopics().getTopic(CONFIG_PAGES_FOOTER).getSimpleValue().toString();
    }

    public Boolean getCustomWorkspaceEnabled() {
        // TODO: Naming
        return topic.getChildTopics().getBoolean(CONFIG_API_ENABLED);
    }

    public String getCustomWorkspaceDescription() {
        return topic.getChildTopics().getTopic(CONFIG_API_DESCRIPTION).getSimpleValue().toString();
    }

    public String getCustomWorkspaceDetails() {
        return topic.getChildTopics().getTopic(CONFIG_API_DETAILS).getSimpleValue().toString();
    }

    public String getCustomWorkspaceUri() {
        return topic.getChildTopics().getTopic(CONFIG_API_WORKSPACE_URI).getSimpleValue().toString();
    }

    public String getStartUrl() {
        return topic.getChildTopics().getTopic(CONFIG_START_PAGE_URL).getSimpleValue().toString();
    }

    public String getHomeUrl() {
        return topic.getChildTopics().getTopic(CONFIG_HOME_PAGE_URL).getSimpleValue().toString();
    }

    public String getLoadingAppHint() {
        return topic.getChildTopics().getTopic(CONFIG_LOADING_HINT).getSimpleValue().toString();
    }

    public String getLoggingOutHint() {
        return topic.getChildTopics().getTopic(CONFIG_LOGGING_OUT_HINT).getSimpleValue().toString();
    }

    public Topic getCustomWorkspaceAssignmentTopic() {
        // Note: It must always be just ONE workspace related to the current module configuration
        return topic.getRelatedTopic(ASSOCIATION, DEFAULT,
                DEFAULT, WORKSPACE);
    }

}
