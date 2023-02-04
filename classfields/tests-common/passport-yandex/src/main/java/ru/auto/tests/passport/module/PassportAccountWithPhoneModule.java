package ru.auto.tests.passport.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.aeonbits.owner.ConfigFactory;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.passport.config.PassportConfig;
import ru.auto.tests.passport.core.DefaultAccountManager;
import ru.auto.tests.passport.core.PassportAccountWithPhoneProvider;
import ru.auto.tests.passport.core.PassportAdaptor;
import ru.auto.tests.passport.manager.AccountManager;

import static com.google.inject.Scopes.SINGLETON;

/**
 * Created by vicdev on 02.11.17.
 */
public class PassportAccountWithPhoneModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new PassportYandexModule());
        install(new PassportAccountWithPhoneProvider());
        install(new PassportAdaptor());
        bind(AccountManager.class).to(DefaultAccountManager.class).in(SINGLETON);
    }

    @Provides
    @Singleton
    public PassportConfig provideConfig() {
        return ConfigFactory.create(PassportConfig.class, System.getProperties(), System.getenv());
    }

    @Provides
    @Singleton
    private AccountKeeper provideAccountKeeper() {
        return new AccountKeeper();
    }
}
