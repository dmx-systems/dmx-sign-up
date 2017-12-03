package org.deepamehta.plugins.signup.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.workspaces.WorkspacesService;

import java.util.logging.Logger;

/**
 * Extends the Sign-up Plugin Configuration about all "API" related configuration options.
 */
public class Migration9 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {

        logger.info("### Assigning \"API Workspace Membership Request Topic\" to System Workspace ###");
        Topic systemWorkspace = wsService.getWorkspace(AccessControlService.SYSTEM_WORKSPACE_URI);
        Topic apiMembershipNote = dm4.getTopicByUri("org.deepamehta.signup.api_membership_requests");
        wsService.assignToWorkspace(apiMembershipNote, systemWorkspace.getId());

    }

}