package ru.auto.tests.realtyapi.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.passport.module.PassportAccountCaptchaNeverWithPhoneModule;
import ru.auto.tests.passport.rules.AddParameterRule;
import ru.auto.tests.passport.rules.DeleteAccountRule;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.provider.RealtyApiV1ProdProvider;
import ru.auto.tests.realtyapi.provider.RealtyApiV1Provider;
import ru.auto.tests.realtyapi.provider.RealtyApiV2ProdProvider;
import ru.auto.tests.realtyapi.provider.RealtyApiV2Provider;
import ru.auto.tests.realtyapi.rules.DeleteOffersRule;
import ru.auto.tests.realtyapi.v1.ApiClient;

import static com.google.inject.Scopes.SINGLETON;

public class RealtyApiModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        rulesBinder.addBinding().to(DeleteAccountRule.class);
        rulesBinder.addBinding().to(DeleteOffersRule.class);
        rulesBinder.addBinding().to(AddParameterRule.class);

        install(new RuleChainModule());
        install(new PassportAccountCaptchaNeverWithPhoneModule());
        install(new BlackboxModule());
        install(new RealtyApiConfigModule());

        bind(ru.auto.tests.realtyapi.v2.ApiClient.class).toProvider(RealtyApiV2Provider.class).in(SINGLETON);
        bind(ru.auto.tests.realtyapi.v2.ApiClient.class).annotatedWith(Prod.class)
                .toProvider(RealtyApiV2ProdProvider.class).in(SINGLETON);
        bind(ApiClient.class).toProvider(RealtyApiV1Provider.class).in(SINGLETON);
        bind(ApiClient.class).annotatedWith(Prod.class).toProvider(RealtyApiV1ProdProvider.class).in(SINGLETON);
    }
}
