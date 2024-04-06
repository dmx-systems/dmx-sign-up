package systems.dmx.signup.usecase;

import systems.dmx.core.Constants;
import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.ModelFactory;

import javax.inject.Inject;
import java.util.logging.Logger;

public class ResetPluginMigrationNrUseCase {

    private Logger logger = Logger.getLogger(getClass().getName());

    private static final String PLUGIN_URI = "systems.dmx.sign-up";

    private static final int PLUGIN_MIGRATION_NR = 2;

    private CoreService dmx;

    @Inject
    ResetPluginMigrationNrUseCase(CoreService dmx) {
        this.dmx = dmx;
    }

    public void invoke() {
        logger.info("Resetting plugin version to " + PLUGIN_MIGRATION_NR);
        dmx.getTopicByUri(PLUGIN_URI)
                .update(dmx.getModelFactory().newChildTopicsModel().set(Constants.PLUGIN_MIGRATION_NR, PLUGIN_MIGRATION_NR));
    }

}