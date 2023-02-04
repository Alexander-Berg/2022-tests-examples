package ru.auto.tests.example.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.aeonbits.owner.ConfigFactory;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.example.ApiClient;
import ru.auto.tests.example.adaptor.ExampleApiAdaptor;
import ru.auto.tests.example.anno.Prod;
import ru.auto.tests.example.config.ExampleApiConfig;
import ru.auto.tests.example.provider.ExampleApiProdProvider;
import ru.auto.tests.example.provider.ExampleApiProvider;

import static com.google.inject.Scopes.SINGLETON;

public class ExampleApiModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);

        install(new RuleChainModule());
        install(new ExampleApiAdaptor());
        install(new ExampleApiConfigModule());

        bind(ApiClient.class).toProvider(ExampleApiProvider.class).in(SINGLETON);
        bind(ApiClient.class).annotatedWith(Prod.class).toProvider(ExampleApiProdProvider.class).in(SINGLETON);
    }
}
