package org.deepamehta.plugins.signup.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.workspaces.WorkspacesService;

import java.util.logging.Logger;

/**
 * Extends the Sign-up Plugin Configuration about a flag to enable/disable the token/mail based confirmation workflow.
 */
public class Migration4 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {

        logger.info("### Extending Sign-up Configuration about \"Email Confirmation Required\" option ###");

        Topic systemWorkspace = wsService.getWorkspace(AccessControlService.SYSTEM_WORKSPACE_URI);
        TopicType tokenConfirmationType = dm4.getTopicType("org.deepamehta.signup.config_email_confirmation");
        wsService.assignTypeToWorkspace(tokenConfirmationType, systemWorkspace.getId());

        TopicType configComposite = dm4.getTopicType("org.deepamehta.signup.configuration");
        configComposite.addAssocDef(mf.newAssociationDefinitionModel(
                "dm4.core.composition_def", "org.deepamehta.signup.configuration",
                "org.deepamehta.signup.config_email_confirmation", "dm4.core.one", "dm4.core.one"));

        Topic standardConfig = dm4.getTopicByUri("org.deepamehta.signup.default_configuration");
        standardConfig.loadChildTopics();
        standardConfig.getChildTopics().set("org.deepamehta.signup.config_email_confirmation", false);

        logger.info("### Setup new Sign-up Config Default: No Email confirmation required during account creation");

    }

}