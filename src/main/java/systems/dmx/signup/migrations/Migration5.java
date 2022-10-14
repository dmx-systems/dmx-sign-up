package systems.dmx.signup.migrations;

import java.util.logging.Logger;

import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.service.Migration;
import systems.dmx.signup.Constants;

/**
 * Extends the Sign-up Plugin Configuration about a "Start Page URL" and a "Home Page URL" for customizing
 * our login resp. the registration dialog (in the case of a needed confirmation).
 */
public class Migration5 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    /* Type names are repeated in here, so that renaming is possible in a future version. */
    private static final String MIGRATION5_SIGNUP_DEFAULT_CONFIGURATION_URI = "dmx.signup.default_configuration";
    private static final String MIGRATION5_SIGN_UP_CONFIG_TYPE_URI = "dmx.signup.configuration";
    private static final String MIGRATION5_CONFIG_START_PAGE_URL = "dmx.signup.start_page_url";
    private static final String MIGRATION5_CONFIG_HOME_PAGE_URL = "dmx.signup.home_page_url";
    private static final String MIGRATION5_CONFIG_LOADING_APP_HINT = "dmx.signup.loading_app_hint";
    private static final String MIGRATION5_CONFIG_LOGGING_OUT_HINT = "dmx.signup.logging_out_hint";

    private static final String DEFAULT_START_PAGE_URL = System.getProperty(Constants.CONFIG_START_PAGE_URL, "/systems.dmx.webclient/");

    private static final String DEFAULT_HOME_PAGE_URL = System.getProperty(Constants.CONFIG_HOME_PAGE_URL, "/systems.dmx.webclient/");

    private static final String DEFAULT_LOADING_APP_HINT = System.getProperty(Constants.CONFIG_LOADING_APP_HINT, "Loading DMX Webclient");

    private static final String DEFAULT_LOGGING_OUT_HINT = System.getProperty(Constants.CONFIG_LOGGING_OUT_HINT, "Logging out..");

    @Override
    public void run() {

        // ### TODO: We have (deliberately) missed to create new Workspace Assignments here.

        logger.info("### Extending Sign-up Configuration about \"Start Page URL\" option ###");
        dmx.createTopicType(mf.newTopicTypeModel(MIGRATION5_CONFIG_START_PAGE_URL,
            "Sign-up: Start Page URL", "dmx.core.text"));

        logger.info("### Extending Sign-up Configuration about \"Home Page URL\" option ###");
        dmx.createTopicType(mf.newTopicTypeModel(MIGRATION5_CONFIG_HOME_PAGE_URL,
            "Sign-up: Home Page URL", "dmx.core.text"));

        logger.info("### Extending Sign-up Configuration about \"Loading App Hint\" option ###");
        dmx.createTopicType(mf.newTopicTypeModel(MIGRATION5_CONFIG_LOADING_APP_HINT,
            "Sign-up: Loading App Hint", "dmx.core.text"));

        logger.info("### Extending Sign-up Configuration about \"Logging Out Hint\" option ###");
        dmx.createTopicType(mf.newTopicTypeModel(MIGRATION5_CONFIG_LOGGING_OUT_HINT,
            "Sign-up: Logging Out Hint", "dmx.core.text"));

        TopicType signupConfigType = dmx.getTopicType(MIGRATION5_SIGN_UP_CONFIG_TYPE_URI);
        signupConfigType.addCompDef(mf.newCompDefModel(
                MIGRATION5_SIGN_UP_CONFIG_TYPE_URI, MIGRATION5_CONFIG_START_PAGE_URL, "dmx.core.one"));
        signupConfigType.addCompDef(mf.newCompDefModel(
                MIGRATION5_SIGN_UP_CONFIG_TYPE_URI, MIGRATION5_CONFIG_HOME_PAGE_URL, "dmx.core.one"));
        signupConfigType.addCompDef(mf.newCompDefModel(
                MIGRATION5_SIGN_UP_CONFIG_TYPE_URI, MIGRATION5_CONFIG_LOADING_APP_HINT, "dmx.core.one"));
        signupConfigType.addCompDef(mf.newCompDefModel(
                MIGRATION5_SIGN_UP_CONFIG_TYPE_URI, MIGRATION5_CONFIG_LOGGING_OUT_HINT, "dmx.core.one"));
        // Set new default config values
        Topic defaultConfiguration = dmx.getTopicByUri(MIGRATION5_SIGNUP_DEFAULT_CONFIGURATION_URI);
        dmx.updateTopic(
                mf.newTopicModel(defaultConfiguration.getId(), 
                        mf.newChildTopicsModel()
                                .set(MIGRATION5_CONFIG_START_PAGE_URL, DEFAULT_START_PAGE_URL)
                )
        );
        dmx.updateTopic(
                mf.newTopicModel(defaultConfiguration.getId(), 
                        mf.newChildTopicsModel()
                                .set(MIGRATION5_CONFIG_HOME_PAGE_URL, DEFAULT_HOME_PAGE_URL)
                )
        );
        dmx.updateTopic(
                mf.newTopicModel(defaultConfiguration.getId(), 
                        mf.newChildTopicsModel()
                                .set(MIGRATION5_CONFIG_LOADING_APP_HINT, DEFAULT_LOADING_APP_HINT)
                )
        );
        dmx.updateTopic(
                mf.newTopicModel(defaultConfiguration.getId(), 
                        mf.newChildTopicsModel()
                                .set(MIGRATION5_CONFIG_LOGGING_OUT_HINT, DEFAULT_LOGGING_OUT_HINT)
                )
        );
    }

}