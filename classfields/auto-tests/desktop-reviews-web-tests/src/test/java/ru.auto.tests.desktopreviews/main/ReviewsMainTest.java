package ru.auto.tests.desktopreviews.main;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsMainPage.REVIEWS_LISTING_SIZE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.MAIN)
@DisplayName("Главная отзывов")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReviewsMainTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps steps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS},
                {MOTO},
                {TRUCKS}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).path(category).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отзыв в списке")
    public void shouldSeeListingReview() {
        screenshotSteps.setWindowSize(1920, 5000);
        steps.onReviewsMainPage().reviewsList().should(hasSize(REVIEWS_LISTING_SIZE));

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onReviewsMainPage().getReview(0));

        urlSteps.setProduction().open();
        steps.onReviewsMainPage().reviewsList().should(hasSize(REVIEWS_LISTING_SIZE));
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onReviewsMainPage().getReview(0));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отзыву")
    @Category({Regression.class})
    public void shouldClickReview() {
        steps.onReviewsMainPage().getReview(0).click();
        urlSteps.switchToNextTab();
        steps.onReviewPage().title()
                .should(hasAttribute("textContent", startsWith("Отзыв владельца ")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по самому полезному отзыву")
    @Category({Regression.class})
    public void shouldClickMostUsefulReview() {
        steps.onReviewsMainPage().topReview("Самый полезный").click();
        steps.onReviewPage().title()
                .should(hasAttribute("textContent", startsWith("Отзыв владельца ")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по самому обсуждаемому отзыву")
    @Category({Regression.class})
    public void shouldClickMostDiscussedReview() {
        steps.onReviewsMainPage().topReview("Самый обсуждаемый").click();
        steps.onReviewPage().title()
                .should(hasAttribute("textContent", startsWith("Отзыв владельца ")));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по автору отзыва")
    @Category({Regression.class})
    public void shouldClickReviewAuthor() {
        steps.onReviewsMainPage().getReview(1).authorUrl().click();
        steps.onReviewPage().title()
                .should(hasAttribute("textContent", "Страница пользователя"));
    }
}