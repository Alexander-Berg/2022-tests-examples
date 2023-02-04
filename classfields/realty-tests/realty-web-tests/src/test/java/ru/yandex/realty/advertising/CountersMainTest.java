package ru.yandex.realty.advertising;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.ADVERTISING;

@DisplayName("Проверяем, что на главной странице есть счетчики")
@Feature(ADVERTISING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Issue("VERTISTEST-562")
public class CountersMainTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ProxySteps proxy;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void openMain() {
        proxy.clearHar();
        urlSteps.testing().open();
        basePageSteps.scrolling(10000, 500);
    }

    @Test
    @Owner(VICDEV)
    @Category({Regression.class, Smoke.class, Production.class})
    @DisplayName("Счетчик tns")
    public void shouldSeeTNSCounter() {
        proxy.shouldSeeRequestInLog(
                containsString("https://www.tns-counter.ru/V13a****yandex_ru/ru/UTF-8/tmsec=yandex_realty/"),
                greaterThanOrEqualTo(1));
    }

    @Test
    @Owner(VICDEV)
    @Category({Regression.class, Smoke.class, Production.class})
    @DisplayName("Счетчик google")
    public void shouldSeeGoogleCounter() {
        proxy.shouldSeeRequestInLog(containsString("https://google-analytics.com"), greaterThanOrEqualTo(1));
    }
}
