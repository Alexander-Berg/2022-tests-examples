package ru.auto.tests.desktop.lk.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы")
@Feature(LK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReviewsTest {

    private final static String ACTIVE_REVIEW_ID = "3867271072784994963";
    private final static String DRAFT_REVIEW_ID = "566989686761442140";

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

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop-lk/UserReviews",
                "desktop-lk/ReviewsAutoId",
                "desktop-lk/ReviewsAutoIdDelete").post();

        urlSteps.testing().path(MY).path(REVIEWS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Отображение заблокированного отзыва")
    public void shouldSeeBlockedReview() {
        screenshotSteps.setWindowSizeForScreenshot();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkReviewsPage().getReview(4));

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkReviewsPage().getReview(4));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }


    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение отзыва без заголовка")
    public void shouldSeeReviewWithoutTitle() {
        basePageSteps.onLkReviewsPage().getReview(2).title().should(hasText("Без заголовка"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по заголовку отзыва")
    public void shouldClickReviewTitle() {
        basePageSteps.onLkReviewsPage().getReview(0).title().click();
        urlSteps.testing().path(REVIEW).path(CARS).path("/zaz/slavuta/4760888/").path(ACTIVE_REVIEW_ID)
                .path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.onLkReviewsPage().getReview(0).button("Редактировать").should(isDisplayed()).click();
        urlSteps.testing().path(CARS).path(REVIEWS).path(EDIT).path(ACTIVE_REVIEW_ID).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Опубликовать»")
    public void shouldClickPublishButton() {
        basePageSteps.onLkReviewsPage().getReview(2).button("Опубликовать").should(isDisplayed()).click();
        urlSteps.testing().path(CARS).path(REVIEWS).path(EDIT).path(DRAFT_REVIEW_ID).path("/").fragment("validate")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Добавить отзыв»")
    public void shouldClickAddReviewButton() {
        basePageSteps.onLkReviewsPage().addReviewButton().should(isDisplayed()).click();
        urlSteps.testing().path(CARS).path(REVIEWS).path(ADD).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаление отзыва")
    public void shouldDeleteReview() {
        int size = basePageSteps.onLkReviewsPage().reviewsList().size();
        String reviewTitle = basePageSteps.onLkReviewsPage().getReview(0).title().getText();
        basePageSteps.onLkReviewsPage().getReview(0).button("Удалить").click();
        basePageSteps.onLkReviewsPage().reviewsList().waitUntil(hasSize(size - 1));
        basePageSteps.onLkReviewsPage().getReview(0).title().waitUntil(not(hasText(reviewTitle)));
        urlSteps.shouldNotSeeDiff();
    }
}
