package org.deepamehta.plugins.signup.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.workspaces.WorkspacesService;
import java.util.logging.Logger;

/**
 * Custom Migration for installing the Climbo specific sign-up plugin config.
 * @author malted
 */
public class Migration13  extends Migration {
    
    private Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {
        logger.info("###### Load Sign-up Configuration Topic");
        Topic config = dm4.getTopicByUri("org.deepamehta.signup.default_configuration");
        logger.info("###### Installed Climbo specific Sign-up Configuraton");
    }
    
}
