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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_MAX_PAGE;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_1920;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@DisplayName("Листинг отзывов - сводка по всем отзывам о модели")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ListingSummaryTest {

    private static final String MARK = "/subaru/";
    private static final String MODEL = "/impreza/";
    private static final String GENERATION = "3492727";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL)
                .addParam("super_gen", GENERATION).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение сводки")
    public void shouldSeeSummary() {
        screenshotSteps.setWindowSize(WIDTH_1920, HEIGHT_MAX_PAGE);

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewsListingPage().summary());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewsListingPage().summary());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Характеристики модели»")
    public void shouldClickSpecificationsUrl() {
        basePageSteps.onReviewsListingPage().summary().url("Характеристики модели").click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path("/3492727/5140442/")
                .path(SPECIFICATIONS).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Все плюсы и минусы»")
    public void shouldClickPlusAndMinusUrl() {
        basePageSteps.onReviewsListingPage().summary().url("Все плюсы и минусы").click();
        basePageSteps.onReviewsListingPage().reviewsPlusMinusPopup().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке на все отзывы о модели")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onReviewsListingPage().summary().allReviewsUrl().click();
        urlSteps.fragment("listing").shouldNotSeeDiff();
    }
}
