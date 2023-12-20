package systems.dmx.signup.usecase;

import org.osgi.framework.BundleContext;
import systems.dmx.ldap.service.LDAPService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GetLdapServiceUseCase {

    @Inject
    GetLdapServiceUseCase() {}

    public OptionalService<LDAPService> invoke(BundleContext bundleContext) {
        return new OptionalService<>(bundleContext, () -> LDAPService.class);
    }

}
