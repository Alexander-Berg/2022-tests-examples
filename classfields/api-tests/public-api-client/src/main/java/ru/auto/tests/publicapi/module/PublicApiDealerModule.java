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
import ru.auto.tests.passport.rules.DeleteAccountRule;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.provider.DealerAccountProvider;
import ru.auto.tests.publicapi.provider.PublicApiProdProvider;
import ru.auto.tests.publicapi.provider.PublicApiProvider;
import ru.auto.tests.publicapi.rules.DeleteAggregatorsRule;
import ru.auto.tests.publicapi.rules.DeleteDealerOffersRule;
import ru.auto.tests.publicapi.utils.OfferKeeper;

import static com.google.inject.Scopes.SINGLETON;
import static ru.auto.tests.commons.guice.CustomScopes.THREAD;

/**
 * Created by dskuznetsov on 25.12.18
 */

public class PublicApiDealerModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        //VERTISTEST-821
        rulesBinder.addBinding().to(AddParameterRule.class);
        rulesBinder.addBinding().to(DeleteDealerOffersRule.class);
        rulesBinder.addBinding().to(DeleteAggregatorsRule.class);

        install(new RuleChainModule());
        install(new PublicApiAdaptor());
        install(new PublicApiConfigModule());
        install(new PublicApiClientModule());

        bind(Account.class).toProvider(DealerAccountProvider.class).in(THREAD);
        bind(OfferKeeper.class).toProvider(OfferKeeper::new).in(SINGLETON);
    }

    @Provides
    @Singleton
    private AccountKeeper provideAccountModule() { return new AccountKeeper(); }
}
