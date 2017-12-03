package org.deepamehta.plugins.signup.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.workspaces.WorkspacesService;
import java.util.List;
import java.util.logging.Logger;

public class Migration2 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {

        Topic pluginTopic = dm4.getTopicByUri("org.deepamehta.sign-up");
        Topic standardConfiguration = dm4.getTopicByUri("org.deepamehta.signup.default_configuration");
        // 1) Assign the (default) "Sign-up Configuration" to the Plugin topic
        List<Association> configs = pluginTopic.getAssociations();
        boolean hasConfiguration = false;
        for (Association assoc : configs) {
            if (assoc.getPlayer1().getTypeUri().equals("org.deepamehta.signup.configuration")) hasConfiguration = true;
        }
        if (!hasConfiguration) {
            logger.info("Sign-up => Assigning default \"Sign-up Configuration\" to \"DeepaMehta 4 Sign up\" Topic");
            Association assoc = dm4.createAssociation(mf.newAssociationModel("dm4.core.association",
                    mf.newTopicRoleModel(pluginTopic.getId(), "dm4.core.default"),
                    mf.newTopicRoleModel(standardConfiguration.getId(), "dm4.core.default")
            ));
            Topic systemWorkspace = wsService.getWorkspace(AccessControlService.SYSTEM_WORKSPACE_URI);
            wsService.assignToWorkspace(assoc, systemWorkspace.getId());
        } else {
            logger.info("Sign-up => NOT assigning \"Sign-up Configuration\" to \"DeepaMehta 4 Sign up\" Topic"
                    + "- Already done!");
        }
        // 2) Set Configuration Topic Workspace Assignment to ("System") (editable for admin)
        Topic systemWorkspace = wsService.getWorkspace(AccessControlService.SYSTEM_WORKSPACE_URI);
        wsService.assignToWorkspace(standardConfiguration, systemWorkspace.getId());

    }

}