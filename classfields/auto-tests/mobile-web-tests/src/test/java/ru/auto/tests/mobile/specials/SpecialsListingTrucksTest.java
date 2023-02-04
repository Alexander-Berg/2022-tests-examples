package ru.auto.tests.mobile.specials;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SPECIAL;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Листинг - спецпредложения")
@Feature(SPECIAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SpecialsListingTrucksTest {

    private static final int ITEMS_CNT = 6;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchTrucksBreadcrumbsEmpty",
                "mobile/SearchTrucksAllEmpty",
                "desktop/SearchTrucksSpecialsAll").post();

        urlSteps.testing().path(MOSKVA).path(TRUCK).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение спецпредложений")
    @Category({Regression.class, Testing.class})
    public void shouldSeeSpecialSales() {
        basePageSteps.onListingPage().specialSales().itemsList().should(hasSize(ITEMS_CNT));
        basePageSteps.onListingPage().specialSales().getSale(0)
                .should(hasText("Санкт-Петербург\n850 000 ₽\nHino Series 300 (Dutro)\n2013 г., 140 000 км"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению")
    @Category({Regression.class, Testing.class})
    public void shouldClickSale() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").update();

        basePageSteps.onListingPage().specialSales().getSale(0).click();
        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path("/hino/300_dutro_l/16541988-e9c7e07a/")
                .shouldNotSeeDiff();
    }
}
