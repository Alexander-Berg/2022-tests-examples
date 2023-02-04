package ru.auto.tests.desktopreviews.review;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.REVIEW)
@DisplayName("Страница отзыва - блок «Оценка отзыва»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ProsAndConsTest {

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

        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GENERATION)
                .path("/4014660/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Блок «Оценка отзыва»")
    public void shouldSeeProsAndCons() {
        screenshotSteps.setWindowSize(1920, 5000);

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().prosAndCons());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().prosAndCons());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Ховер на оценке автора отзыва")
    @Category({Regression.class})
    public void shouldHoverAuthorRating() {
        basePageSteps.onReviewPage().prosAndCons().authorRating().hover();
        basePageSteps.onReviewPage().tooltip().waitUntil(hasText("Рейтинг модели 4,2 из 5"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке на все отзывы")
    @Category({Regression.class})
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onReviewPage().prosAndCons().allReviewsUrl().click();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(GENERATION)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Все плюсы и минусы»")
    @Category({Regression.class})
    public void shouldClickPlusAndMinusButton() {
        basePageSteps.onReviewPage().prosAndCons().plusAndMinusButton().click();
        basePageSteps.onReviewPage().prosAndCons().reviewsPlusMinusPopup().waitUntil(isDisplayed());
    }
}