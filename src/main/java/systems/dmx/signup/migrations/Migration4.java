package systems.dmx.signup.migrations;

import java.util.logging.Logger;

import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.workspaces.WorkspacesService;

/**
 * Extends the Sign-up Plugin Configuration about a flag to enable/disable the token/mail based confirmation workflow.
 */
public class Migration4 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    private static final String MIGRATION4_API_MEMBER_SHIP_NOTE_URI = "dmx.signup.api_membership_requests"

    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {
        // Defunct: Migrated item has now moved int platform conf ("dmx.signup.confirm_email_address")

        if (dmx.getTopicByUri(MIGRATION4_API_MEMBER_SHIP_NOTE_URI) != null) {
            // if this note is present, then it means a previous run of the same plugin version did migration 18 and 19
            // and then set the plugin version back. The plugin is not supposed to be run again, so we abort here.
            logger.warning(
                    "Detected run of dmx-sign-up 3.1 migrations after the migrations had been run previously.\n" +
                            "This is not supposed to be done. To not cause data corruption the execution will fail here.\n" +
                            "You can safely upgrade to dmx-sign-up 3.2 now.");
        }

    }

}