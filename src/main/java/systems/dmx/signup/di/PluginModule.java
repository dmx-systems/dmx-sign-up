package systems.dmx.signup.di;

import dagger.Binds;
import dagger.Module;
import systems.dmx.signup.mapper.IsValidEmailAdressMapper;
import systems.dmx.signup.mapper.NewAccountDataMapper;

import javax.inject.Singleton;

@Module
public interface PluginModule {

    @Binds
    @Singleton
    NewAccountDataMapper provideNewAccountDataMapper(NewAccountDataMapper impl);

    @Binds
    @Singleton
    IsValidEmailAdressMapper provideIsValidEmailAdressMapper(IsValidEmailAdressMapper impl);

}
