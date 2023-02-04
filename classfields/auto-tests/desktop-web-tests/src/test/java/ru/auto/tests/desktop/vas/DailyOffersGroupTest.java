package ru.auto.tests.desktop.vas;

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
import ru.auto.tests.desktop.consts.Pages;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.RELATED;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("«Предложения дня» на группе")
@Feature(RELATED)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DailyOffersGroupTest {

    private static final int VISIBLE_ITEMS_CNT = 4;
    private static final String PATH = "/kia/optima/21342050-21342121/";
    private static final String DAILY_OFFER = "/1082682634-c030085c/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsGroupComplectations",
                "desktop/SearchCarsSpecialsKiaOptima",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechParam").post();

        urlSteps.testing().path(CARS).path(NEW).path(Pages.GROUP).path(PATH).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onGroupPage().footer(), 0, -300);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Листание предложений")
    public void shouldSlideOffers() {
        basePageSteps.onGroupPage().horizontalDailyOffers().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onGroupPage().horizontalDailyOffers().prevButton().should(not(isDisplayed()));
        basePageSteps.onGroupPage().horizontalDailyOffers().nextButton().click();
        basePageSteps.onGroupPage().horizontalDailyOffers().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onGroupPage().horizontalDailyOffers().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onGroupPage().horizontalDailyOffers().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> isDisplayed());
        basePageSteps.onGroupPage().horizontalDailyOffers().prevButton().waitUntil(not(isDisplayed()));
        basePageSteps.onGroupPage().horizontalDailyOffers().nextButton().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по предложению")
    public void shouldClickOffer() {
        basePageSteps.onGroupPage().horizontalDailyOffers().getItem(1).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(NEW).path(Pages.GROUP).path("/hyundai/tucson/21357358/21357426/")
                .path(DAILY_OFFER).shouldNotSeeDiff();
    }
}
