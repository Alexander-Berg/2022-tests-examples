package ru.auto.tests.realty.vos2.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.aeonbits.owner.ConfigFactory;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.module.PassportAccountModule;
import ru.auto.tests.passport.rules.AddParameterRule;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;
import ru.auto.tests.realty.vos2.anno.Compare;
import ru.auto.tests.realty.vos2.anno.CompareOffers;
import ru.auto.tests.realty.vos2.anno.Prod;
import ru.auto.tests.realty.vos2.config.Vos2ApiConfig;
import ru.auto.tests.realty.vos2.provider.AccountCompareOffersProvider;
import ru.auto.tests.realty.vos2.provider.AccountCompareProvider;
import ru.auto.tests.realty.vos2.provider.Vos2ApiProdProvider;
import ru.auto.tests.realty.vos2.provider.Vos2ApiProvider;

import static ru.auto.tests.commons.guice.CustomScopes.THREAD;

public class Vos2ApiCompareModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        rulesBinder.addBinding().to(AddParameterRule.class);

        install(new RuleChainModule());
        install(new PassportAccountModule());
        install(new Vos2ApiAdaptor());

        bind(Account.class).annotatedWith(CompareOffers.class).toProvider(AccountCompareOffersProvider.class).in(THREAD);
        bind(Account.class).annotatedWith(Compare.class).toProvider(AccountCompareProvider.class).in(THREAD);
        bind(ApiClient.class).toProvider(Vos2ApiProvider.class).in(THREAD);
        bind(ApiClient.class).annotatedWith(Prod.class).toProvider(Vos2ApiProdProvider.class).in(THREAD);
    }

    @Provides
    @Singleton
    private Vos2ApiConfig provideConfig() {
        return ConfigFactory.create(Vos2ApiConfig.class, System.getProperties(), System.getenv());
    }
}
