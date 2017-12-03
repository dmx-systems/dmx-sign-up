package org.deepamehta.plugins.signup.migrations;

import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.Migration;

import java.util.logging.Logger;

/**
 * Extends the Sign-up Plugin Configuration about a "Start Page URL" and a "Home Page URL" for customizing
 * our login resp. the registration dialog (in the case of a needed confirmation).
 */
public class Migration5 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void run() {

        // ### TODO: We have (deliberately) missed to create new Workspace Assignments here.

        logger.info("### Extending Sign-up Configuration about \"Start Page URL\" option ###");
        dm4.createTopicType(mf.newTopicTypeModel("org.deepamehta.signup.start_page_url",
            "Sign-up: Start Page URL", "dm4.core.text"));

        logger.info("### Extending Sign-up Configuration about \"Home Page URL\" option ###");
        dm4.createTopicType(mf.newTopicTypeModel("org.deepamehta.signup.home_page_url",
            "Sign-up: Home Page URL", "dm4.core.text"));

        logger.info("### Extending Sign-up Configuration about \"Loading App Hint\" option ###");
        dm4.createTopicType(mf.newTopicTypeModel("org.deepamehta.signup.loading_app_hint",
            "Sign-up: Loading App Hint", "dm4.core.text"));

        logger.info("### Extending Sign-up Configuration about \"Logging Out Hint\" option ###");
        dm4.createTopicType(mf.newTopicTypeModel("org.deepamehta.signup.logging_out_hint",
            "Sign-up: Logging Out Hint", "dm4.core.text"));

        TopicType signupConfigType = dm4.getTopicType("org.deepamehta.signup.configuration");
        signupConfigType.addAssocDef(mf.newAssociationDefinitionModel(
                "dm4.core.composition_def", "org.deepamehta.signup.configuration",
                "org.deepamehta.signup.start_page_url", "dm4.core.one", "dm4.core.one"));
        signupConfigType.addAssocDef(mf.newAssociationDefinitionModel(
                "dm4.core.composition_def", "org.deepamehta.signup.configuration",
                "org.deepamehta.signup.home_page_url", "dm4.core.one", "dm4.core.one"));
        signupConfigType.addAssocDef(mf.newAssociationDefinitionModel(
                "dm4.core.composition_def", "org.deepamehta.signup.configuration",
                "org.deepamehta.signup.loading_app_hint", "dm4.core.one", "dm4.core.one"));
        signupConfigType.addAssocDef(mf.newAssociationDefinitionModel(
                "dm4.core.composition_def", "org.deepamehta.signup.configuration",
                "org.deepamehta.signup.logging_out_hint", "dm4.core.one", "dm4.core.one"));

        Topic defaultConfiguration = dm4.getTopicByUri("org.deepamehta.signup.default_configuration");
        defaultConfiguration.loadChildTopics();
        defaultConfiguration.getChildTopics().set("org.deepamehta.signup.start_page_url", "/de.deepamehta.webclient/");
        defaultConfiguration.getChildTopics().set("org.deepamehta.signup.home_page_url", "/de.deepamehta.webclient/");
        defaultConfiguration.getChildTopics().set("org.deepamehta.signup.loading_app_hint", "Loading Webclient..");
        defaultConfiguration.getChildTopics().set("org.deepamehta.signup.logging_out_hint", "Logging out..");

    }

}