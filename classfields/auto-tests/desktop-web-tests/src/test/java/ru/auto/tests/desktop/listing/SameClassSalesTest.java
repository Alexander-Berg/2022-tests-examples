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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Листинг - автомобили того же класса")
@Feature(LISTING)
public class SameClassSalesTest {

    private static final String MARK = "audi";
    private static final String MODEL = "a5";
    private static final String GENERATION = "2305408";
    private static final Integer VISIBLE_ITEMS_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(USED).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().pager(), 0, 500);
        basePageSteps.onListingPage().sameClassSales().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Листание объявлений")
    public void shouldSlideModels() {
        basePageSteps.onListingPage().sameClassSales().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> isDisplayed());
        basePageSteps.onListingPage().sameClassSales().prevButton().should(not(isDisplayed()));
        basePageSteps.onListingPage().sameClassSales().nextButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().sameClassSales().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> isDisplayed());
        basePageSteps.onListingPage().sameClassSales().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().sameClassSales().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> isDisplayed());
        basePageSteps.onListingPage().sameClassSales().nextButton().waitUntil(isDisplayed());
        basePageSteps.onListingPage().sameClassSales().prevButton().should(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        String title = basePageSteps.onListingPage().sameClassSales().getItem(0).title().getText();
        ;
        basePageSteps.onListingPage().sameClassSales().getItem(0).waitUntil(isDisplayed()).hover().click();
        basePageSteps.switchToNextTab();
        basePageSteps.onCardPage().cardHeader().title().waitUntil(hasText(startsWith(title)));
    }
}
