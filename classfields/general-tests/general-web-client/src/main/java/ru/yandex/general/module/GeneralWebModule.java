package ru.yandex.general.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.aeonbits.owner.ConfigFactory;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.commons.modules.WebDriverModule;
import ru.yandex.general.config.GeneralWebConfig;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.rules.SetCookieRule;
import ru.yandex.general.webdriver.WebDriverResource;

public class GeneralWebModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);

        rulesBinder.addBinding().to(WebDriverResource.class);
        rulesBinder.addBinding().to(SetCookieRule.class);

        bind(MockRule.class);

        install(new WebDriverModule());
        install(new RuleChainModule());

    }

    @Provides
    @Singleton
    public GeneralWebConfig provideGeneralWebConfig() {
        return ConfigFactory.create(GeneralWebConfig.class, System.getProperties(), System.getenv());
    }

}
