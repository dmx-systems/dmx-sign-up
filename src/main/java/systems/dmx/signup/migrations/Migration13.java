package systems.dmx.signup.migrations;

import java.util.logging.Logger;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.workspaces.WorkspacesService;

public class Migration12 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {
        logger.info("###### Move Topic \"Api Membership Request Helper Note\" to \"System\" workspace");
        Topic apiMembershipNote = dmx.getTopicByUri("dmx.signup.api_membership_requests");
        wsService.assignToWorkspace(apiMembershipNote, dmx.getPrivilegedAccess().getSystemWorkspaceId());
        logger.info("###### \"Api Membership Request Helper Note\" topic migration to \"System\" workspace complete");
    }

}