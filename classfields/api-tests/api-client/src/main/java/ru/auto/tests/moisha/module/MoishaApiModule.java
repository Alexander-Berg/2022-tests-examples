package ru.auto.tests.moisha.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.moisha.ApiClient;
import ru.auto.tests.moisha.adaptor.MoishaApiAdaptor;
import ru.auto.tests.moisha.anno.Prod;
import ru.auto.tests.moisha.provider.MoishaApiProdProvider;
import ru.auto.tests.moisha.provider.MoishaApiProvider;

import static com.google.inject.Scopes.SINGLETON;

public class MoishaApiModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);

        install(new RuleChainModule());
        install(new MoishaApiAdaptor());
        install(new MoishaApiConfigModule());

        bind(ApiClient.class).toProvider(MoishaApiProvider.class).in(SINGLETON);
        bind(ApiClient.class).annotatedWith(Prod.class).toProvider(MoishaApiProdProvider.class).in(SINGLETON);
    }
}
