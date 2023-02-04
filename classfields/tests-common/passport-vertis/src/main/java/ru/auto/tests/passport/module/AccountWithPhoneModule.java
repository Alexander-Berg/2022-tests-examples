package ru.auto.tests.passport.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.aeonbits.owner.ConfigFactory;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;
import ru.auto.tests.passport.config.PassportApiConfig;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.passport.manager.DefaultAccountManager;
import ru.auto.tests.passport.providers.AccountWithPhoneProvider;

import static com.google.inject.Scopes.SINGLETON;

/**
 * Created by vicdev on 15.09.17.
 */
public class AccountWithPhoneModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new PassportApiModule());
        install(new PassportApiAdaptor());
        install(new AccountWithPhoneProvider());

        bind(AccountManager.class).to(DefaultAccountManager.class).in(SINGLETON);
    }

    @Provides
    @Singleton
    public PassportApiConfig provideConfig() {
        return ConfigFactory.create(PassportApiConfig.class, System.getProperties(), System.getenv());
    }

    @Provides
    @Singleton
    private AccountKeeper provideAccountModule() {
        return new AccountKeeper();
    }
}
