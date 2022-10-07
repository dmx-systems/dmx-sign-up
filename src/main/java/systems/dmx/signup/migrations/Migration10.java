package systems.dmx.signup.migrations;

import java.util.List;

import java.util.logging.Logger;
import static systems.dmx.accesscontrol.Constants.USERNAME;
import systems.dmx.core.Assoc;
import static systems.dmx.core.Constants.CHILD;
import static systems.dmx.core.Constants.PARENT;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import static systems.dmx.signup.Constants.*;
import static systems.dmx.signup.Constants.CONFIG_CSS_PATH;
import static systems.dmx.signup.Constants.SIGNUP_SYMBOLIC_NAME;
import static systems.dmx.signup.Constants.SIGN_UP_CONFIG_TYPE_URI;
import static systems.dmx.signup.Constants.USER_MAILBOX_EDGE_TYPE;
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
        long administrationWsId = dmx.getPrivilegedAccess().getAdminWorkspaceId();

        logger.info("###### Migrate all relevant Sign-up Configration Topics to \"Administration\" Workspace");
        // 1 Re-Assign "Standard Configuration" Composition Topic to "Administration"
        Topic standardConfiguration = dmx.getTopicByUri("dmx.signup.default_configuration");
        wsService.assignToWorkspace(standardConfiguration, administrationWsId);
        standardConfiguration.loadChildTopics();
        RelatedTopic webAppTitle = standardConfiguration.getChildTopics().getTopic(CONFIG_WEBAPP_TITLE);
        RelatedTopic logoPath = standardConfiguration.getChildTopics().getTopic(CONFIG_LOGO_PATH);
        RelatedTopic cssPath = standardConfiguration.getChildTopics().getTopic(CONFIG_CSS_PATH);
        RelatedTopic projectTitle = standardConfiguration.getChildTopics().getTopic(CONFIG_PROJECT_TITLE);
        RelatedTopic tosLabel = standardConfiguration.getChildTopics().getTopic(CONFIG_TOS_LABEL);
        RelatedTopic tosDetail = standardConfiguration.getChildTopics().getTopic(CONFIG_TOS_DETAILS);
        RelatedTopic pdLabel = standardConfiguration.getChildTopics().getTopic(CONFIG_PD_LABEL);
        RelatedTopic pdDetail = standardConfiguration.getChildTopics().getTopic(CONFIG_PD_DETAILS);
        RelatedTopic readMoreUrl = standardConfiguration.getChildTopics().getTopic(CONFIG_READ_MORE_URL);
        RelatedTopic pagesFooter = standardConfiguration.getChildTopics().getTopic(CONFIG_PAGES_FOOTER);
        // RelatedTopic apiDescr = standardConfiguration.getChildTopics().getTopic("dmx.signup.config_api_description");
        // RelatedTopic apiDetails = standardConfiguration.getChildTopics().getTopic("dmx.signup.config_api_details");
        // RelatedTopic apiEnabled = standardConfiguration.getChildTopics().getTopic("dmx.signup.config_api_enabled");
        // RelatedTopic apiWsURI = standardConfiguration.getChildTopics().getTopic("dmx.signup.config_api_workspace_uri");
        wsService.assignToWorkspace(webAppTitle, administrationWsId);
        wsService.assignToWorkspace(webAppTitle.getRelatingAssoc(), administrationWsId);
        // wsService.assignToWorkspace(logoPath, administrationWsId);
        // wsService.assignToWorkspace(logoPath.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(cssPath, administrationWsId);
        wsService.assignToWorkspace(cssPath.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(projectTitle, administrationWsId);
        wsService.assignToWorkspace(projectTitle.getRelatingAssoc(), administrationWsId);
        // wsService.assignToWorkspace(tosLabel, administrationWsId);
        // wsService.assignToWorkspace(tosLabel.getRelatingAssoc(), administrationWsId);
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
        /** wsService.assignToWorkspace(apiDescr, administrationWsId);
        wsService.assignToWorkspace(apiDescr.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(apiDetails, administrationWsId);
        wsService.assignToWorkspace(apiDetails.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(apiEnabled, administrationWsId);
        wsService.assignToWorkspace(apiEnabled.getRelatingAssoc(), administrationWsId);
        wsService.assignToWorkspace(apiWsURI, administrationWsId);
        wsService.assignToWorkspace(apiWsURI.getRelatingAssoc(), administrationWsId); **/
        // 3 Create Plugin <-> Standard Configuration Association to "Administration"
        Topic pluginTopic = dmx.getTopicByUri(SIGNUP_SYMBOLIC_NAME);
        // 3.1) Fixme: Probably not yet there on a fresh install.
        if (pluginTopic != null) {
            List<Assoc> configs = pluginTopic.getAssocs();
            for (Assoc assoc : configs) {
                if (assoc.getPlayer1().getDMXObject().getTypeUri().equals(SIGN_UP_CONFIG_TYPE_URI) ||
                    assoc.getPlayer2().getDMXObject().getTypeUri().equals(SIGN_UP_CONFIG_TYPE_URI)) {
                    wsService.assignToWorkspace(assoc, administrationWsId);
                    assoc.setSimpleValue(new SimpleValue("Active Configuration"));
                }
            }
        }
        // 4 Move Topic "Api Membership Request Helper Note" to "Administration"
        Topic apiMembershipNote = dmx.getTopicByUri("dmx.signup.api_membership_requests");
        wsService.assignToWorkspace(apiMembershipNote, administrationWsId);
        // 5 Move all email address into "administration" workspace
        logger.info("###### Migrate all users Email Addresses to \"Administration\" Workspace");
        List<Topic> emails = dmx.getTopicsByType("dmx.contacts.email_address");
        for (Topic email : emails) {
            RelatedTopic username = email.getRelatedTopic(USER_MAILBOX_EDGE_TYPE, CHILD,
                PARENT, USERNAME);
            if (username != null) wsService.assignToWorkspace(email, administrationWsId);
        }
        logger.info("###### Email Address topic migration to \"Administration\" Workspace complete");
    }

}