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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.REVIEW)
@DisplayName("Страница отзыва - плавающая панель")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class StickyHeaderTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "reviews/ReviewsAutoCars",
                "reviews/SearchCarsBreadcrumbsUazPatriot",
                "reviews/ReviewAutoOpinionLike",
                "reviews/ReviewAutoOpinionDislike").post();

        screenshotSteps.setWindowSize(1920, 3000);

        urlSteps.testing().path(REVIEW).path(CARS).path("/uaz/patriot/2309645/4014660/").open();
        steps.scrollDown(1500);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение плавающей панели")
    public void shouldSeeStickyHeader() throws InterruptedException {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onReviewPage().stickyHeader());

        urlSteps.setProduction().open();
        steps.scrollDown(1500);
        TimeUnit.SECONDS.sleep(2);

        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onReviewPage().stickyHeader());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    @Category({Regression.class})
    public void shouldClickTitle() {
        steps.onReviewPage().stickyHeader().title().click();
        steps.onReviewPage().stickyHeader().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Палец вверх»")
    @Category({Regression.class})
    public void shouldClickUpButton() {
        steps.onReviewPage().stickyHeader().unpressedUpButton().click();
        steps.onReviewPage().stickyHeader().unpressedUpButton().waitUntil(not(isDisplayed()));
        steps.onReviewPage().stickyHeader().pressedUpButton().waitUntil(isDisplayed());
        steps.onReviewPage().stickyHeader().unpressedDownButton().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Палец вниз»")
    @Category({Regression.class})
    public void shouldClickDownButton() {
        steps.onReviewPage().stickyHeader().unpressedDownButton().click();
        steps.onReviewPage().stickyHeader().unpressedDownButton().waitUntil(not(isDisplayed()));
        steps.onReviewPage().stickyHeader().pressedDownButton().waitUntil(isDisplayed());
        steps.onReviewPage().stickyHeader().unpressedUpButton().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке N комментариев")
    @Category({Regression.class})
    public void shouldClickCommentsButton() {
        steps.onReviewPage().stickyHeader().commentsButton().click();
    }
}