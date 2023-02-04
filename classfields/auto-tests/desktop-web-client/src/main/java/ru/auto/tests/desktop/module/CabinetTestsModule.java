package ru.auto.tests.desktop.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import org.aeonbits.owner.ConfigFactory;
import org.junit.rules.TestRule;
import ru.auto.tests.api.module.SearcherModule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.commons.modules.WebDriverModule;
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.rule.CabinetDesktopWebDriverResource;
import ru.auto.tests.desktop.rule.SetCookiesRule;
import ru.auto.tests.passport.rules.AddParameterRule;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.vos2.module.VosModule;


public class CabinetTestsModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        rulesBinder.addBinding().to(AddParameterRule.class);
        rulesBinder.addBinding().to(CabinetDesktopWebDriverResource.class);
        rulesBinder.addBinding().to(SetCookiesRule.class);

        install(new WebDriverModule());
        install(new RuleChainModule());
        install(new SearcherModule());
        install(new VosModule());
        install(new PublicApiModule());
    }

    @Provides
    private DesktopConfig provideDesktopConfig() {
        return ConfigFactory.create(DesktopConfig.class, System.getProperties(), System.getenv());
    }
}
