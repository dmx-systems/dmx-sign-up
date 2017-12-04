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
        logger.info("###### Load Default Sign-up Configuration Topic");
        Topic config = dm4.getTopicByUri("org.deepamehta.signup.default_configuration");
        config.getChildTopics().set("org.deepamehta.signup.start_page_url", "/");
        config.getChildTopics().set("org.deepamehta.signup.home_page_url", "/");
        config.getChildTopics().set("org.deepamehta.signup.loading_app_hint", "Loading Climbo..");
        config.getChildTopics().set("org.deepamehta.signup.logging_out_hint", "Logging out..");
        config.getChildTopics().set("org.deepamehta.signup.config_webapp_logo_path", "/org.deepamehta.sign-up/images/place_pink.png");
        // ### config.getChildTopics().set("org.deepamehta.signup.config_custom_css_path", "");
        logger.info("###### Installed Climbo specific Sign-up Configuraton");
    }
    
}
