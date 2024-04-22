package systems.dmx.signup.di;

import dagger.Component;
import systems.dmx.accesscontrol.AccessControlService;
import systems.dmx.core.service.CoreService;
import systems.dmx.sendmail.SendmailService;
import systems.dmx.signup.mapper.IsValidEmailAdressMapper;
import systems.dmx.signup.mapper.NewAccountDataMapper;
import systems.dmx.signup.repository.ConfigurationRepository;
import systems.dmx.signup.usecase.*;
import systems.dmx.workspaces.WorkspacesService;

import javax.inject.Singleton;

@Singleton
@Component(dependencies = {CoreService.class, AccessControlService.class, WorkspacesService.class, SendmailService.class})
public interface SignupComponent {

    NewAccountDataMapper newAccountDataMapper();

    IsValidEmailAdressMapper isValidEmailAdressMapper();

    GetLdapServiceUseCase getLdapServiceUseCase();

    GetAccountCreationPasswordUseCase getAccountCreationPasswordUseCase();

    HasAccountCreationPrivilegeUseCase hasAccountCreationPrivilegeUseCase();

    IsPasswordComplexEnoughUseCase isPasswordComplexEnoughUseCase();

    LogAndVerifyConfigurationUseCase logAndVerifyConfigurationUseCase();

    SendMailUseCase sendMailUseCase();
}
