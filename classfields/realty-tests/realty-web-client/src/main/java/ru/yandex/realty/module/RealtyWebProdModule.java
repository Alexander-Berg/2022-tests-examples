package ru.yandex.realty.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.commons.modules.WebDriverModule;
import ru.yandex.realty.webdriver.WebDriverResource;

public class RealtyWebProdModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);

        rulesBinder.addBinding().to(WebDriverResource.class);

        install(new WebDriverModule());
        install(new RuleChainModule());
        install(new RealtyWebConfigModule());
    }
}

