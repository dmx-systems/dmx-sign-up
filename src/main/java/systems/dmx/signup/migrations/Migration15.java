package systems.dmx.signup.migrations;

import java.util.logging.Logger;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.accesscontrol.Constants;
import systems.dmx.core.Topic;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.core.service.accesscontrol.SharingMode;
import static systems.dmx.signup.Constants.DISPLAY_NAME_WS_NAME;
import static systems.dmx.signup.Constants.DISPLAY_NAME_WS_URI;
import systems.dmx.workspaces.WorkspacesService;

public class Migration15 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private WorkspacesService wsService;
    @Inject
    private AccessControlService acService;

    @Override
    public void run() {
        Topic ws = wsService.createWorkspace(DISPLAY_NAME_WS_NAME, DISPLAY_NAME_WS_URI, 
                SharingMode.COLLABORATIVE);
        acService.setWorkspaceOwner(ws, Constants.ADMIN_USERNAME);
    }
}