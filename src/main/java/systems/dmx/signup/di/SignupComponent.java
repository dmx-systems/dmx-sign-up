package systems.dmx.signup.di;

import dagger.Component;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.core.service.CoreService;
import systems.dmx.signup.mapper.IsValidEmailAdressMapper;
import systems.dmx.signup.mapper.NewAccountDataMapper;
import systems.dmx.signup.usecase.GetAccountCreationPasswordUseCase;
import systems.dmx.signup.usecase.GetLdapServiceUseCase;
import systems.dmx.signup.usecase.HasAccountCreationPrivilegeUseCase;
import systems.dmx.workspaces.WorkspacesService;

import javax.inject.Singleton;

@Singleton
@Component(modules = {},
        dependencies = {CoreService.class, AccessControlService.class, WorkspacesService.class})
public interface SignupComponent {

    NewAccountDataMapper newAccountDataMapper();

    IsValidEmailAdressMapper isValidEmailAdressMapper();

    GetLdapServiceUseCase getLdapServiceUseCase();

    GetAccountCreationPasswordUseCase getAccountCreationPasswordUseCase();

    HasAccountCreationPrivilegeUseCase hasAccountCreationPrivilegeUseCase();
}
