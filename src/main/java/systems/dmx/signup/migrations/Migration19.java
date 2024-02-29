package systems.dmx.signup.migrations;

import systems.dmx.core.Constants;
import systems.dmx.core.service.Migration;

import java.util.logging.Logger;

public class Migration19 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    private static final String MIGRATION19_PLUGIN_URI = "systems.dmx.sign-up";

    private static final int MIGRATION19_PLUGIN_MIGRATION_NR = 2;

    @Override
    public void run() {
        logger.info("Resetting plugin version to " + MIGRATION19_PLUGIN_MIGRATION_NR);
        dmx.getTopicByUri(MIGRATION19_PLUGIN_URI)
                .update(mf.newChildTopicsModel().set(Constants.PLUGIN_MIGRATION_NR, MIGRATION19_PLUGIN_MIGRATION_NR));

        logger.info("*** Completed the final migration. Now uninstall dmx-sign 3.1 and put 3.2 in place. ***");
        logger.warning("Do not start DMX again with this version of the sign-up plugin!");
    }

}