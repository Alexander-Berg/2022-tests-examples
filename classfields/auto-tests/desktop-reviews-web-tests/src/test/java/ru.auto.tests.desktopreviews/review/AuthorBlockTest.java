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

import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.PROFILE;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.REVIEW)
@DisplayName("Страница отзыва - блок автора отзыва")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AuthorBlockTest {

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
                "reviews/ReviewAutoOpinionLike",
                "reviews/ReviewAutoOpinionDislike").post();

        urlSteps.testing().path(REVIEW).path(CARS).path("/uaz/patriot/2309645/4014660/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение блока")
    public void shouldSeeAuthorBlock() {
        screenshotSteps.setWindowSize(1920, 5000);

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().authorBlock());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().authorBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по автору отзыва")
    @Category({Regression.class})
    public void shouldClickAuthorUrl() {
        basePageSteps.onReviewPage().authorBlock().authorUrl().click();
        urlSteps.testing().path(PROFILE).path("3562413").path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Да» в блоке «Понравился отзыв?»")
    @Category({Regression.class})
    public void shouldClickRateYesButton() {
        basePageSteps.onReviewPage().authorBlock().rateYesButton().click();
        basePageSteps.onReviewPage().authorBlock().rateYesButton().waitUntil(hasText("369"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Нет» в блоке «Понравился отзыв?»")
    @Category({Regression.class})
    public void shouldClickRateNoButton() {
        basePageSteps.onReviewPage().authorBlock().rateNoButton().click();
        basePageSteps.onReviewPage().authorBlock().rateNoButton().waitUntil(hasText("22"));
    }

    @Test
    @DisplayName("Поделяшки")
    @Category({Regression.class})
    public void shouldShare() {
        String currentUrl = urlSteps.getCurrentUrl();
        String shareText = "Посмотрите отзыв автовладельца UAZ PATRIOT 2309645 на Авто.ру";

        basePageSteps.onReviewPage().vkButton().should(isDisplayed()).should(hasAttribute("href",
                startsWith("https://vk.com"))).click();
        urlSteps.shouldSeeCertainNumberOfTabs(3);
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(startsWith("https://oauth.vk.com/authorize"));
    }
}