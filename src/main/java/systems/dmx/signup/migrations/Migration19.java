package systems.dmx.signup.migrations;

import systems.dmx.core.Constants;
import systems.dmx.core.service.Migration;

import java.util.logging.Logger;

public class Migration19 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void run() {
        logger.info("*** Completed the final migration. Now uninstall dmx-sign 3.1 and put 3.2 in place. ***");
        logger.warning("Do not start DMX again with this version of the sign-up plugin!");
    }

}