package ru.auto.tests.mobilereviews.review;

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
import ru.auto.tests.desktop.mobile.page.mobilereviews.ReviewsListingPage;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@DisplayName("Страница отзыва - блок «Оценка отзыва»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class RatingsTest {

    private static final String MARK = "/uaz/";
    private static final String MODEL = "/patriot/";
    private static final String GENERATION = "/2309645/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "reviews/ReviewsAutoCars",
                "reviews/SearchCarsBreadcrumbsUazPatriot",
                "reviews/ReviewAutoFeaturesCars").post();

        basePageSteps.setWindowHeight(6000);
        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GENERATION)
                .path("/4014660/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Блок «Отзывы владельцев»")
    public void shouldSeeRatings() {

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().ratings());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().ratings());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке на все отзывы")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onReviewPage().ratings().allReviewsUrl().click();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(GENERATION).path("/")
                .addParam("sort", ReviewsListingPage.SortBy.RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Характеристики модели»")
    public void shouldClickSpecificationsUrl() {
        basePageSteps.onReviewPage().ratings().url("Характеристики модели").click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path("/2309645/2309646/")
                .path(SPECIFICATIONS).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Все плюсы и минусы»")
    public void shouldClickPlusAndMinusUrl() {
        basePageSteps.onReviewPage().ratings().url("Все плюсы и минусы").click();
        basePageSteps.onReviewPage().reviewsPlusMinusPopup().waitUntil(isDisplayed());
    }
}
