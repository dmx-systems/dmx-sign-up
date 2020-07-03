package systems.dmx.signup.migrations;

import java.util.List;

import java.util.logging.Logger;
import systems.dmx.core.Assoc;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.workspaces.WorkspacesService;

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
        long administrationWsId = dmx.getPrivilegedAccess().getAdministrationWorkspaceId();
        // 1 Re-Assign "Standard Configuration" Composition Topic to "Administration"
        Topic standardConfiguration = dmx.getTopicByUri("org.deepamehta.signup.default_configuration");
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
        wsService.assignToWorkspace(adminMailbox, administrationWsId);
        wsService.assignToWorkspace(adminMailbox.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(fromMailbox, administrationWsId);
        wsService.assignToWorkspace(fromMailbox.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(emailConfirmaton, administrationWsId);
        wsService.assignToWorkspace(emailConfirmaton.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(apiDescr, administrationWsId);
        wsService.assignToWorkspace(apiDescr.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(apiDetails, administrationWsId);
        wsService.assignToWorkspace(apiDetails.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(apiEnabled, administrationWsId);
        wsService.assignToWorkspace(apiEnabled.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(apiWsURI, administrationWsId);
        wsService.assignToWorkspace(apiWsURI.getRelatingAssoc(), administrationWsId);
        // 2 Delete Child Topic Type "System" Workspace Assignment
        TopicType tokenConfirmationType = dmx.getTopicType("org.deepamehta.signup.config_email_confirmation");
        List<Assoc> tokenConfirmationTypeAssignments = tokenConfirmationType.getAssocs();
        for (Assoc assoc : tokenConfirmationTypeAssignments) {
            if (assoc.getPlayer1().getDMXObject().getTypeUri().equals("dmx.workspaces.workspace") ||
                assoc.getPlayer2().getDMXObject().getTypeUri().equals("dmx.workspaces.workspace")) {
                assoc.delete();
            }
        }
        // 3 Create Plugin <-> Standard Configuration Association to "Administration"
        Topic pluginTopic = dmx.getTopicByUri("org.deepamehta.sign-up");
        List<Assoc> configs = pluginTopic.getAssocs();
        for (Assoc assoc : configs) {
            if (assoc.getPlayer1().getDMXObject().getTypeUri().equals("org.deepamehta.signup.configuration") ||
                assoc.getPlayer2().getDMXObject().getTypeUri().equals("org.deepamehta.signup.configuration")) {
                wsService.assignToWorkspace(assoc, administrationWsId);
                assoc.setSimpleValue(new SimpleValue("Active Configuration"));
            }
        }
        // 4 Move Topic "Api Membership Request Helper Note" to "Administration"
        Topic apiMembershipNote = dmx.getTopicByUri("org.deepamehta.signup.api_membership_requests");
        wsService.assignToWorkspace(apiMembershipNote, administrationWsId);
        // 5 Move all email address into "administration" workspace
        logger.info("###### Migrate all users Email Addresses to \"Administration\" Workspace");
        List<Topic> emails = dmx.getTopicsByType("dmx.contacts.email_address");
        for (Topic email : emails) {
            RelatedTopic username = email.getRelatedTopic("org.deepamehta.signup.user_mailbox", "dmx.core.child",
                "dmx.core.parent", "dmx.accesscontrol.username");
            if (username != null) wsService.assignToWorkspace(email, administrationWsId);
        }
        logger.info("###### Email Address topic migration to \"Administration\" Workspace complete");
    }

}