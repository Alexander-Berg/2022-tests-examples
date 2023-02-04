package ru.auto.tests.desktop.listing;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг All - блок «Ещё в других городах» (бесконечный листинг)")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class InfiniteListingAllTest {

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsTwoSales",
                "desktop/SearchCarsOfferLocatorCountersTotalCount",
                "desktop/SearchCarsAllExcludeGeoRadius200",
                "desktop/SearchCarsAllExcludeGeoRadius300").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().getSale(1), 0, 0);
        basePageSteps.onListingPage().infiniteListing().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока")
    public void shouldSeeInfiniteListing() {
        basePageSteps.onListingPage().infiniteListing().title().should(hasText("Ещё 87 527 в других городах"));
        basePageSteps.onListingPage().infiniteListing().salesList().should(hasSize(3));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        mockRule.with("desktop/OfferCarsUsedUser").update();

        basePageSteps.onListingPage().infiniteListing().getSale(0).nameLink().click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/1076842087-f1e84/")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Подгрузка объявлений")
    public void shouldLoadSales() {
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().infiniteListing().getSale(2), 0, 0);
        basePageSteps.onListingPage().infiniteListing().salesList().waitUntil(hasSize(6));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное")
    public void shouldAddToFavorites() {
        basePageSteps.onListingPage().infiniteListing().getSale(0).hover();
        basePageSteps.onListingPage().infiniteListing().getSale(0).toolBar().favoriteButton().click();
        basePageSteps.onListingPage().authPopup().waitUntil(isDisplayed());
    }
}