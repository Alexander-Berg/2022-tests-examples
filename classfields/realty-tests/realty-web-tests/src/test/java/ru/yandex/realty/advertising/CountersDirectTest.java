package ru.yandex.realty.advertising;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Dimension;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.realty.consts.Counters.AN_YANDEX_RU_COUNT;
import static ru.yandex.realty.consts.Counters.AN_YANDEX_RU_RBTCOUNT;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.ADVERTISING;


@DisplayName("Счетчики an.yandex.ru. Проверяем, что с различных страниц уходит реклама")
@Feature(ADVERTISING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class CountersDirectTest {

    private static final int SCROLL_DOWN = 10000;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ProxySteps proxy;

    @Before
    public void before() {
        proxy.clearHar();
        basePageSteps.getDriver().manage().window().setSize(new Dimension(1800, 1800));
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Счётчики «https://an.yandex.ru/count» на главной")
    public void shouldSeeCountRequestInHarLogOnMain() {
        urlSteps.testing().open();
        basePageSteps.moveCursor(basePageSteps.onMainPage().rtbAdBlock());
        proxy.shouldSeeRequestInLog(startsWith(AN_YANDEX_RU_COUNT), equalTo(1));
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Счётчики «https://an.yandex.ru/count» на вторичке")
    public void shouldSeeCountRequestInHarLogOnListing() {
        urlSteps.testing().path("/sankt-peterburg/kupit/kvartira/").open();
        basePageSteps.scrolling(SCROLL_DOWN, 200);
        proxy.shouldSeeRequestInLog(startsWith(AN_YANDEX_RU_COUNT), greaterThanOrEqualTo(7));
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Счётчики «https://an.yandex.ru/rtbcount» на главной")
    public void shouldSeeRBTCountRequestInHarLogOnMain() {
        urlSteps.testing().open();
        basePageSteps.moveCursor(basePageSteps.onMainPage().rtbAdBlock());
        proxy.shouldSeeRequestInLog(startsWith(AN_YANDEX_RU_RBTCOUNT), greaterThanOrEqualTo(1));
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Счётчики «https://an.yandex.ru/rtbcount» на вторичке")
    public void shouldSeeRBTCountRequestInHarLogOnListing() {
        urlSteps.testing().path("/sankt-peterburg/kupit/kvartira/").open();
        basePageSteps.moveCursor(basePageSteps.onMainPage().rtbAdBlock());
        proxy.shouldSeeRequestInLog(startsWith(AN_YANDEX_RU_RBTCOUNT), greaterThanOrEqualTo(1));
    }
}
