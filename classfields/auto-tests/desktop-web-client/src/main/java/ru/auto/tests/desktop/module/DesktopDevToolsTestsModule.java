package ru.auto.tests.desktop.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import org.aeonbits.owner.ConfigFactory;
import org.junit.rules.TestRule;
import ru.auto.tests.api.module.SearcherModule;
import ru.auto.tests.commons.guice.CustomScopes;
import ru.auto.tests.commons.modules.LocatorModule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.commons.modules.WebDriverModule;
import ru.auto.tests.commons.rule.LocatorsRule;
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.managers.DevToolsManager;
import ru.auto.tests.desktop.managers.DevToolsManagerImpl;
import ru.auto.tests.desktop.rule.DesktopWebDriverResource;
import ru.auto.tests.desktop.rule.DevToolsResource;
import ru.auto.tests.desktop.rule.SetCookiesRule;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.vos2.module.VosModule;


public class DesktopDevToolsTestsModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        rulesBinder.addBinding().to(DesktopWebDriverResource.class);
        rulesBinder.addBinding().to(SetCookiesRule.class);
        rulesBinder.addBinding().to(LocatorsRule.class);
        rulesBinder.addBinding().to(DevToolsResource.class);

        bind(DevToolsManager.class).to(DevToolsManagerImpl.class).in(CustomScopes.THREAD);

        install(new WebDriverModule());
        install(new RuleChainModule());
        install(new SearcherModule());
        install(new LocatorModule());
        install(new VosModule());
        install(new PublicApiModule());
    }

    @Provides
    private DesktopConfig provideDesktopConfig() {
        return ConfigFactory.create(DesktopConfig.class, System.getProperties(), System.getenv());
    }
}
