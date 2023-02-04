package ru.auto.tests.mobile.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DEALER_NET;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Карточка дилера - хлебные крошки")
@Feature(AutoruFeatures.DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CardBreadcrumbsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "mobile/SearchCarsAllDealerId",
                "desktop/Salon").post();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение хлебных крошек")
    public void shouldSeeBreadcrumbs() {
        basePageSteps.onDealerCardPage().breadcrumbs()
                .should(hasText("Все дилерыСеть АВИЛОНАвилон Mercedes-Benz Воздвиженка"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по «Все дилеры»")
    public void shouldClickAllDealersUrl() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").update();

        basePageSteps.onDealerCardPage().breadcrumb("Все дилеры").click();
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на дилерскую сеть")
    public void shouldClickNet() {
        mockRule.with("desktop/ProxyPublicApi",
                "desktop/ProxySearcher").update();

        basePageSteps.onDealerCardPage().breadcrumb("Сеть АВИЛОН").click();
        urlSteps.testing().path(DEALER_NET).path("/avilon/").shouldNotSeeDiff();
    }
}
