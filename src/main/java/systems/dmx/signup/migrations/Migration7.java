package systems.dmx.signup.migrations;


import java.util.logging.Logger;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.signup.Constants;
import systems.dmx.workspaces.WorkspacesService;

/**
 * Extends the Sign-up Plugin Configuration about "API" related sign-up configuration options.
 */
public class Migration7 extends Migration {

    private Logger logger = Logger.getLogger(getClass().getName());

    private static final String MIGRATION7_SIGNUP_DEFAULT_CONFIGURATION_URI = "dmx.signup.default_configuration";
    private static final String MIGRATION7_SIGN_UP_CONFIG_TYPE_URI = "dmx.signup.configuration";
    private static final String MIGRATION7_CONFIG_API_ENABLED = "dmx.signup.config_api_enabled";
    private static final String MIGRATION7_CONFIG_API_DESCRIPTION = "dmx.signup.config_api_description";
    private static final String MIGRATION7_CONFIG_API_DETAILS = "dmx.signup.config_api_details";
    private static final String MIGRATION7_CONFIG_API_WORKSPACE_URI = "dmx.signup.config_api_workspace_uri";

    private static final boolean DEFAULT_API_ENABLED = Boolean.parseBoolean(System.getProperty(Constants.CONFIG_API_ENABLED, "false"));

    private static final String DEFAULT_API_WORKSPACE_URI = System.getProperty(Constants.CONFIG_API_WORKSPACE_URI, "undefined");

    private static final String DEFAULT_API_DESCRIPTION = System.getProperty(Constants.CONFIG_API_DESCRIPTION, "API unavailable");

    private static final String DEFAULT_API_DETAILS = System.getProperty(Constants.CONFIG_API_DETAILS, "No API, no Terms of service.");

    // ### TODO: We have (deliberately) missed to create new Workspace Assignments (for these new child topics) here.
    @Inject
    private WorkspacesService wsService;

    @Override
    public void run() {

        logger.info("### Extending Sign-up Configuration about \"API Workspace\" configuration options ###");

        TopicType signupConfigType = dmx.getTopicType(MIGRATION7_SIGN_UP_CONFIG_TYPE_URI);
        //
        signupConfigType.addCompDef(mf.newCompDefModel(
                MIGRATION7_SIGN_UP_CONFIG_TYPE_URI,
                MIGRATION7_CONFIG_API_ENABLED, "dmx.core.one"));
        signupConfigType.addCompDef(mf.newCompDefModel(
                MIGRATION7_SIGN_UP_CONFIG_TYPE_URI,
                MIGRATION7_CONFIG_API_WORKSPACE_URI, "dmx.core.one"));
        signupConfigType.addCompDef(mf.newCompDefModel(
                MIGRATION7_SIGN_UP_CONFIG_TYPE_URI,
                MIGRATION7_CONFIG_API_DESCRIPTION, "dmx.core.one"));
        signupConfigType.addCompDef(mf.newCompDefModel(
                MIGRATION7_SIGN_UP_CONFIG_TYPE_URI,
                MIGRATION7_CONFIG_API_DETAILS, "dmx.core.one"));

        // Set new default config values
        Topic defaultConfiguration = dmx.getTopicByUri(MIGRATION7_SIGNUP_DEFAULT_CONFIGURATION_URI);
        dmx.updateTopic(
                mf.newTopicModel(defaultConfiguration.getId(), 
                        mf.newChildTopicsModel()
                                .set(MIGRATION7_CONFIG_API_DETAILS, DEFAULT_API_DETAILS)
                )
        );
        dmx.updateTopic(
                mf.newTopicModel(defaultConfiguration.getId(), 
                        mf.newChildTopicsModel()
                                .set(MIGRATION7_CONFIG_API_ENABLED, DEFAULT_API_ENABLED)
                )
        );
        dmx.updateTopic(
                mf.newTopicModel(defaultConfiguration.getId(), 
                        mf.newChildTopicsModel()
                                .set(MIGRATION7_CONFIG_API_DESCRIPTION, DEFAULT_API_DESCRIPTION)
                )
        );
        dmx.updateTopic(
                mf.newTopicModel(defaultConfiguration.getId(), 
                        mf.newChildTopicsModel()
                                .set(MIGRATION7_CONFIG_API_WORKSPACE_URI, DEFAULT_API_WORKSPACE_URI)
                )
        );

    }

}