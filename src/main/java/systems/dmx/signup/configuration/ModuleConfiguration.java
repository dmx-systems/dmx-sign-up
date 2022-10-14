package systems.dmx.signup.configuration;

import systems.dmx.core.Topic;

import static systems.dmx.core.Constants.ASSOCIATION;
import static systems.dmx.core.Constants.DEFAULT;
import static systems.dmx.signup.Constants.*;
import static systems.dmx.workspaces.Constants.WORKSPACE;

/**
 * Provides the module configuration options. The individual values are tried to be loaded from
 * the signup configuration first and from the DMX configuration as a fallback.
 *
 * If no value is provided string fall back to the empty string and boolean default to false.
 */
public class ModuleConfiguration {

    private final Topic topic;

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

    private String getString(String key) {
        return topic.getChildTopics().getString(key);
    }

    private boolean getBoolean(String key) {
        return topic.getChildTopics().getBoolean(key);
    }

    public String getProjectTitle() {
        return getString(CONFIG_PROJECT_TITLE);
    }

    public String getWebAppTitle() {
        return getString(CONFIG_WEBAPP_TITLE);
    }

    public String getLogoPath() {
        return getString(CONFIG_LOGO_PATH);
    }

    public String getCssPath() {
        return getString(CONFIG_CUSTOM_CSS_PATH);
    }

    public String getReadMoreUrl() {
        return getString(CONFIG_READ_MORE_URL);
    }
    public String getTosLabel() {
        return getString(CONFIG_TOS_LABEL);
    }

    public String getTosDetails() {
        return getString(CONFIG_TOS_DETAILS);
    }

    public String getPdLabel() {
        return getString(CONFIG_PD_LABEL);
    }

    public String getPdDetails() {
        return getString(CONFIG_PD_DETAILS);
    }

    public String getPagesFooter() {
        return getString(CONFIG_PAGES_FOOTER);
    }

    public Boolean getApiEnabled() {
        return getBoolean(CONFIG_API_ENABLED);
    }

    public String getApiDescription() {
        return getString(CONFIG_API_DESCRIPTION);
    }

    public String getApiDetails() {
        return getString(CONFIG_API_DETAILS);
    }

    public String getApiWorkspaceUri() {
        return getString(CONFIG_API_WORKSPACE_URI);
    }

    public String getStartUrl() {
        return getString(CONFIG_START_PAGE_URL);
    }

    public String getHomeUrl() {
        return getString(CONFIG_HOME_PAGE_URL);
    }

    public String getLoadingAppHint() {
        return getString(CONFIG_LOADING_APP_HINT);
    }

    public String getLoggingOutHint() {
        return getString(CONFIG_LOGGING_OUT_HINT);
    }

    public Topic getCustomWorkspaceAssignmentTopic() {
        // Note: It must always be just ONE workspace related to the current module configuration
        return topic.getRelatedTopic(ASSOCIATION, DEFAULT,
                DEFAULT, WORKSPACE);
    }

}
