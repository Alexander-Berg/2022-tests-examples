package ru.auto.tests.mobilereviews.listing;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@DisplayName("Предложения о продаже")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ListingSalesTest {

    private static final String MARK = "gaz";
    private static final String MODEL = "9715";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Before
    public void before() {
        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).open();
        cookieSteps.setCookieForBaseDomain("promo-header-shown", "1");
        cookieSteps.setCookieForBaseDomain("promo-header-counter", "5");
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onReviewPage().footer(), 0, 0);
        basePageSteps.onReviewsListingPage().sales().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению")
    @Category({Regression.class})
    public void shouldClickSale() {
        basePageSteps.onReviewsListingPage().sales().getSale(0).click();
        basePageSteps.switchToNextTab();
        basePageSteps.onCardPage().price().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Смотреть все»")
    @Category({Regression.class})
    public void shouldClickShowAllButton() {
        basePageSteps.onReviewsListingPage().sales().showAllButton().hover().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(ALL).shouldNotSeeDiff();
    }
}
