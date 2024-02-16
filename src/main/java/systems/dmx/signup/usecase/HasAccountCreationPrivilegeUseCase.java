package systems.dmx.signup.usecase;

import systems.dmx.core.service.CoreService;
import systems.dmx.core.service.accesscontrol.AccessControlException;
import systems.dmx.workspaces.WorkspacesService;

import javax.inject.Inject;
import javax.inject.Singleton;

import static systems.dmx.signup.configuration.SignUpConfigOptions.CONFIG_ACCOUNT_CREATION_AUTH_WS_URI;

@Singleton
public class HasAccountCreationPrivilegeUseCase {

    private final CoreService dmx;
    private final WorkspacesService workspacesService;

    @Inject
    HasAccountCreationPrivilegeUseCase(CoreService dmx, WorkspacesService workspacesService) {
        this.dmx = dmx;
        this.workspacesService = workspacesService;
    }

    public boolean invoke() {
        try {
            checkAccountCreation();
            return true;
        } catch (AccessControlException ace) {
            return false;
        } catch (RuntimeException re) {
            // Deals with unexpected behavior of DMX: On missing read permission RuntimeException is thrown
            return false;
        }
    }

    // TODO: Public for historical reasons. Move into its own usecase
    public void checkAccountCreation() {
        if (isAccountCreationWorkspaceUriConfigured()) {
            try {
                checkAccountCreationWorkspaceWriteAccess();
            } catch (AccessControlException ace) {
                checkAdministrationWorkspaceWriteAccess();
            } catch (RuntimeException re) {
                // Deals with unexpected behavior of DMX: On missing read permission RuntimeException is thrown
                checkAdministrationWorkspaceWriteAccess();
            }
        } else {
            checkAdministrationWorkspaceWriteAccess();
        }
    }

    private void checkAdministrationWorkspaceWriteAccess() {
        dmx.getTopic(dmx.getPrivilegedAccess().getAdminWorkspaceId()).checkWriteAccess();
    }

    private boolean isAccountCreationWorkspaceUriConfigured() {
        return !CONFIG_ACCOUNT_CREATION_AUTH_WS_URI.isEmpty();
    }

    private void checkAccountCreationWorkspaceWriteAccess() {
        dmx.getTopic(workspacesService.getWorkspace(CONFIG_ACCOUNT_CREATION_AUTH_WS_URI).getId()).checkWriteAccess();
    }

}
