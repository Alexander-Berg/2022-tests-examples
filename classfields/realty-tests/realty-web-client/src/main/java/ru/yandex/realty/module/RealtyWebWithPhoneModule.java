package ru.yandex.realty.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.LocatorModule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.commons.modules.WebDriverModule;
import ru.auto.tests.commons.rule.LocatorsRule;
import ru.auto.tests.passport.rules.AddParameterRule;
import ru.auto.tests.passport.rules.DeleteAccountRule;
import ru.yandex.realty.rules.AdBlockRule;
import ru.yandex.realty.rules.DeleteOffersRule;
import ru.yandex.realty.rules.DeleteSubscriptionsRule;
import ru.yandex.realty.rules.MockRule;
import ru.yandex.realty.rules.ServiceUnavailableRule;
import ru.yandex.realty.rules.SetCookieRule;
import ru.yandex.realty.webdriver.WebDriverResource;

/**
 * Created by vicdev on 10.07.17.
 */
public class RealtyWebWithPhoneModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        rulesBinder.addBinding().to(AdBlockRule.class);
        rulesBinder.addBinding().to(WebDriverResource.class);
        rulesBinder.addBinding().to(SetCookieRule.class);
        rulesBinder.addBinding().to(LocatorsRule.class);
//        rulesBinder.addBinding().to(MarkerResource.class);
        rulesBinder.addBinding().to(AddParameterRule.class);
        rulesBinder.addBinding().to(DeleteAccountRule.class);
        rulesBinder.addBinding().to(DeleteSubscriptionsRule.class);
        rulesBinder.addBinding().to(DeleteOffersRule.class);
        rulesBinder.addBinding().to(ServiceUnavailableRule.class);
        rulesBinder.addBinding().to(MockRule.class);

//        install(new MarkerModule());
        install(new RealtyApiWithPhoneModule());
        install(new WebDriverModule());
        install(new LocatorModule());
        install(new RuleChainModule());
        install(new RealtyWebConfigModule());
    }
}
