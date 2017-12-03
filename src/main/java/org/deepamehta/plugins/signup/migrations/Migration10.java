package org.deepamehta.plugins.signup.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.workspaces.WorkspacesService;
import java.util.List;

import java.util.logging.Logger;

/**
 * Extends the Sign-up Plugin Configuration about all "API" related configuration options.
 */
public class Migration10 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {

        logger.info("###### Migrate all relevant Sign-up Configration Topics to \"Administration\" Workspace");
        long administrationWsId = dm4.getAccessControl().getAdministrationWorkspaceId();
        // 1 Re-Assign "Standard Configuration" Composition Topic to "Administration"
        Topic standardConfiguration = dm4.getTopicByUri("org.deepamehta.signup.default_configuration");
        wsService.assignToWorkspace(standardConfiguration, administrationWsId);
        RelatedTopic webAppTitle = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_webapp_title");
        RelatedTopic logoPath = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_webapp_logo_path");
        RelatedTopic cssPath = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_custom_css_path");
        RelatedTopic projectTitle = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_project_title");
        RelatedTopic tosLabel = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_tos_label");
        RelatedTopic tosDetail = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_tos_detail");
        RelatedTopic pdLabel = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_pd_label");
        RelatedTopic pdDetail = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_pd_detail");
        RelatedTopic readMoreUrl = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_read_more_url");
        RelatedTopic pagesFooter = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_pages_footer");
        RelatedTopic adminMailbox = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_admin_mailbox");
        RelatedTopic fromMailbox = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_from_mailbox");
        RelatedTopic emailConfirmaton = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_email_confirmation");
        RelatedTopic apiDescr = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_api_description");
        RelatedTopic apiDetails = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_api_details");
        RelatedTopic apiEnabled = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_api_enabled");
        RelatedTopic apiWsURI = standardConfiguration.getChildTopics().getTopic("org.deepamehta.signup.config_api_workspace_uri");
        wsService.assignToWorkspace(webAppTitle, administrationWsId);
        wsService.assignToWorkspace(webAppTitle.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(logoPath, administrationWsId);
        wsService.assignToWorkspace(logoPath.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(cssPath, administrationWsId);
        wsService.assignToWorkspace(cssPath.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(projectTitle, administrationWsId);
        wsService.assignToWorkspace(projectTitle.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(tosLabel, administrationWsId);
        wsService.assignToWorkspace(tosLabel.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(tosDetail, administrationWsId);
        wsService.assignToWorkspace(tosDetail.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(pdLabel, administrationWsId);
        wsService.assignToWorkspace(pdLabel.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(pdDetail, administrationWsId);
        wsService.assignToWorkspace(pdDetail.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(readMoreUrl, administrationWsId);
        wsService.assignToWorkspace(readMoreUrl.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(pagesFooter, administrationWsId);
        wsService.assignToWorkspace(pagesFooter.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(adminMailbox, administrationWsId);
        wsService.assignToWorkspace(adminMailbox.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(fromMailbox, administrationWsId);
        wsService.assignToWorkspace(fromMailbox.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(emailConfirmaton, administrationWsId);
        wsService.assignToWorkspace(emailConfirmaton.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(apiDescr, administrationWsId);
        wsService.assignToWorkspace(apiDescr.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(apiDetails, administrationWsId);
        wsService.assignToWorkspace(apiDetails.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(apiEnabled, administrationWsId);
        wsService.assignToWorkspace(apiEnabled.getRelatingAssociation(), administrationWsId);
        wsService.assignToWorkspace(apiWsURI, administrationWsId);
        wsService.assignToWorkspace(apiWsURI.getRelatingAssociation(), administrationWsId);
        // 2 Delete Child Topic Type "System" Workspace Assignment
        TopicType tokenConfirmationType = dm4.getTopicType("org.deepamehta.signup.config_email_confirmation");
        List<Association> tokenConfirmationTypeAssignments = tokenConfirmationType.getAssociations();
        for (Association assoc : tokenConfirmationTypeAssignments) {
            if (assoc.getPlayer1().getTypeUri().equals("dm4.workspaces.workspace") ||
                assoc.getPlayer2().getTypeUri().equals("dm4.workspaces.workspace")) {
                assoc.delete();
            }
        }
        // 3 Create Plugin <-> Standard Configuration Association to "Administration"
        Topic pluginTopic = dm4.getTopicByUri("org.deepamehta.sign-up");
        List<Association> configs = pluginTopic.getAssociations();
        for (Association assoc : configs) {
            if (assoc.getPlayer1().getTypeUri().equals("org.deepamehta.signup.configuration") ||
                assoc.getPlayer2().getTypeUri().equals("org.deepamehta.signup.configuration")) {
                wsService.assignToWorkspace(assoc, administrationWsId);
                assoc.setSimpleValue(new SimpleValue("Active Configuration"));
            }
        }
        // 4 Move Topic "Api Membership Request Helper Note" to "Administration"
        Topic apiMembershipNote = dm4.getTopicByUri("org.deepamehta.signup.api_membership_requests");
        wsService.assignToWorkspace(apiMembershipNote, administrationWsId);
        // 5 Move all email address into "administration" workspace
        logger.info("###### Migrate all users Email Addresses to \"Administration\" Workspace");
        List<Topic> emails = dm4.getTopicsByType("dm4.contacts.email_address");
        for (Topic email : emails) {
            RelatedTopic username = email.getRelatedTopic("org.deepamehta.signup.user_mailbox", "dm4.core.child",
                "dm4.core.parent", "dm4.accesscontrol.username");
            if (username != null) wsService.assignToWorkspace(email, administrationWsId);
        }
        logger.info("###### Email Address topic migration to \"Administration\" Workspace complete");
    }

}