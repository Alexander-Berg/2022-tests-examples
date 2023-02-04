package ru.yandex.webmaster.tests;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import org.aeonbits.owner.ConfigFactory;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.commons.modules.WebDriverModule;
import ru.auto.tests.commons.rule.WebDriverResource;

public class DefaultModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);

        rulesBinder.addBinding().to(WebDriverResource.class);

        install(new WebDriverModule());
        install(new RuleChainModule());
    }

    @Provides
    private WebMasterConfig provideConfig() {
        return ConfigFactory.create(WebMasterConfig.class, System.getProperties(), System.getenv());
    }
}
