package ru.auto.tests.publicapi.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.passport.rules.AddParameterRule;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.provider.AccountWithDataProvider;
import ru.auto.tests.publicapi.provider.PublicApiProdProvider;
import ru.auto.tests.publicapi.provider.PublicApiProvider;
import ru.auto.tests.publicapi.rules.DeleteCarOffersRule;

import static com.google.inject.Scopes.SINGLETON;
import static ru.auto.tests.commons.guice.CustomScopes.THREAD;

/**
 * Created by dskuznetsov on 26.07.18
 */

public class PublicApiWithDataModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        //VERTISTEST-821
        rulesBinder.addBinding().to(AddParameterRule.class);
        rulesBinder.addBinding().to(DeleteCarOffersRule.class);

        install(new RuleChainModule());
        install(new PublicApiAdaptor());
        install(new PublicApiConfigModule());
        install(new PublicApiClientModule());

        bind(Account.class).toProvider(AccountWithDataProvider.class).in(THREAD);
    }

    @Provides
    @Singleton
    private AccountKeeper provideAccountModule() {
        return new AccountKeeper();
    }
}
