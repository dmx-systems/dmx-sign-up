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

    public Topic getCustomWorkspaceAssignmentTopic() {
        // Note: It must always be just ONE workspace related to the current module configuration
        return topic.getRelatedTopic(ASSOCIATION, DEFAULT,
                DEFAULT, WORKSPACE);
    }

}
