package org.deepamehta.plugins.signup.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.workspaces.WorkspacesService;

import java.util.logging.Logger;

/**
 * Assign four left over Sign-up Plugin Configuration topic childs to "Administration" workspace.
 */
public class Migration11 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {
        logger.info("###### Assign four more Sign-up Configration child topics to \"Administration\" Workspace");
        long administrationWsId = dm4.getAccessControl().getAdministrationWorkspaceId();
        Topic standardConfiguration = dm4.getTopicByUri("org.deepamehta.signup.default_configuration");
        wsService.assignToWorkspace(standardConfiguration, administrationWsId);
        RelatedTopic loadingAppHint = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.loading_app_hint");
        RelatedTopic loggingOutHint = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.logging_out_hint");
        RelatedTopic startPageUrl= standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.start_page_url");
        RelatedTopic homePageUrl= standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.home_page_url");
        wsService.assignToWorkspace(loadingAppHint, administrationWsId);
        wsService.assignToWorkspace(loadingAppHint.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(loggingOutHint, administrationWsId);
        wsService.assignToWorkspace(loggingOutHint.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(startPageUrl, administrationWsId);
        wsService.assignToWorkspace(startPageUrl.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(homePageUrl, administrationWsId);
        wsService.assignToWorkspace(homePageUrl.getRelatingAssociation(), administrationWsId);
    }

}