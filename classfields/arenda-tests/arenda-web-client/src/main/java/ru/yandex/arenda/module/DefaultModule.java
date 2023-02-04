package ru.yandex.arenda.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.aeonbits.owner.ConfigFactory;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.commons.modules.WebDriverModule;
import ru.auto.tests.passport.rules.AddParameterRule;
import ru.auto.tests.passport.rules.DeleteAccountRule;
import ru.yandex.arenda.account.FlatsKeeper;
import ru.yandex.arenda.config.ArendaWebConfig;
import ru.yandex.arenda.rule.DeleteFlatRule;
import ru.yandex.arenda.rule.DeleteUserRule;
import ru.yandex.arenda.rule.LogTestNameRule;
import ru.yandex.arenda.webdriver.WebDriverResource;

public class DefaultModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);

        rulesBinder.addBinding().to(WebDriverResource.class);
        rulesBinder.addBinding().to(LogTestNameRule.class);
        rulesBinder.addBinding().to(AddParameterRule.class);
        rulesBinder.addBinding().to(DeleteAccountRule.class);
        rulesBinder.addBinding().to(DeleteUserRule.class);
        rulesBinder.addBinding().to(DeleteFlatRule.class);

        install(new WebDriverModule());
        install(new RuleChainModule());
    }

    @Provides
    private ArendaWebConfig provideConfig() {
        return ConfigFactory.create(ArendaWebConfig.class, System.getProperties(), System.getenv());
    }

    @Provides
    @Singleton
    private FlatsKeeper provideFlatsKeeper() {
        return new FlatsKeeper();
    }
}
