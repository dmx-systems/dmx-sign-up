package org.deepamehta.plugins.signup.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.workspaces.WorkspacesService;

import java.util.logging.Logger;

/**
 * Extends the Sign-up Plugin Configuration about "API" related sign-up configuration options.
 */
public class Migration7 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    // ### TODO: We have (deliberately) missed to create new Workspace Assignments (for these new child topics) here.
    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {

        logger.info("### Extending Sign-up Configuration about \"API Workspace\" configuration options ###");

        TopicType signupConfigType = dm4.getTopicType("org.deepamehta.signup.configuration");
        //
        signupConfigType.addAssocDef(mf.newAssociationDefinitionModel(
                "dm4.core.composition_def", "org.deepamehta.signup.configuration",
                "org.deepamehta.signup.config_api_enabled", "dm4.core.one", "dm4.core.one"));
        signupConfigType.addAssocDef(mf.newAssociationDefinitionModel(
                "dm4.core.composition_def", "org.deepamehta.signup.configuration",
                "org.deepamehta.signup.config_api_workspace_uri", "dm4.core.one", "dm4.core.one"));
        signupConfigType.addAssocDef(mf.newAssociationDefinitionModel(
                "dm4.core.composition_def", "org.deepamehta.signup.configuration",
                "org.deepamehta.signup.config_api_description", "dm4.core.one", "dm4.core.one"));
        signupConfigType.addAssocDef(mf.newAssociationDefinitionModel(
                "dm4.core.composition_def", "org.deepamehta.signup.configuration",
                "org.deepamehta.signup.config_api_details", "dm4.core.one", "dm4.core.one"));

        Topic defaultConfiguration = dm4.getTopicByUri("org.deepamehta.signup.default_configuration");
        defaultConfiguration.loadChildTopics();
        defaultConfiguration.getChildTopics().set("org.deepamehta.signup.config_api_description", "API unavailable");
        defaultConfiguration.getChildTopics().set("org.deepamehta.signup.config_api_details", "No API, no Terms of service.");
        defaultConfiguration.getChildTopics().set("org.deepamehta.signup.config_api_enabled", false);
        defaultConfiguration.getChildTopics().set("org.deepamehta.signup.config_api_workspace_uri", "undefined");

    }

}