package systems.dmx.signup.migrations;

import java.util.List;
import java.util.logging.Logger;
import static systems.dmx.accesscontrol.Constants.USERNAME;
import systems.dmx.core.Assoc;
import static systems.dmx.core.Constants.CHILD;
import static systems.dmx.core.Constants.PARENT;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import static systems.dmx.signup.SignupPlugin.MAILBOX_TYPE_URI;
import systems.dmx.workspaces.WorkspacesService;

public class Migration13 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {
        logger.info("###### Migrating \"User Mailbox\" edges...");
        List<Topic> users = dmx.getTopicsByType(USERNAME);
        for (Topic user : users) {
            RelatedTopic emailAddress = user.getRelatedTopic("org.deepamehta.signup.user_mailbox", PARENT, CHILD, MAILBOX_TYPE_URI);
            if (emailAddress != null) {
                logger.info("###### Creating new \"User Mailbox\" edge for existing email address");
                Assoc newMailEdge = dmx.createAssoc(mf.newAssocModel("dmx.base.user_mailbox",
                        mf.newTopicPlayerModel(user.getId(), PARENT),
                        mf.newTopicPlayerModel(emailAddress.getId(), CHILD)
                ));
                logger.info("###### Moving new \"User Mailbox\" edge into \"System\" workspace");
                dmx.getPrivilegedAccess().assignToWorkspace(newMailEdge, dmx.getPrivilegedAccess().getSystemWorkspaceId());
                logger.info("###### Removing outdated \"User Mailbox\" edge");
                emailAddress.getRelatingAssoc().delete();
            }
        }
        logger.info("###### Deleting outdated \"User Mailbox\" association type");
        dmx.deleteAssocType("org.deepamehta.signup.user_mailbox");
    }
}