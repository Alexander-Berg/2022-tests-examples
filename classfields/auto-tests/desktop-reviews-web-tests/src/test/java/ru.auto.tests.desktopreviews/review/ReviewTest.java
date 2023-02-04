package ru.auto.tests.desktopreviews.review;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.REVIEW)
@DisplayName("Страница отзыва")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReviewTest {

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String reviewMock;

    @Parameterized.Parameter(2)
    public String breadcrumbsMock;

    @Parameterized.Parameter(3)
    public String path;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "reviews/ReviewsAutoCars", "reviews/SearchCarsBreadcrumbsUazPatriot",
                        "/uaz/patriot/2309645/4014660/"},
                {TRUCKS, "reviews/ReviewsAutoTrucks", "reviews/SearchTrucksBreadcrumbsFotonAumark10xx",
                        "/truck/foton/aumark_10xx/4033391/"},
                {MOTO, "reviews/ReviewsAutoMoto", "reviews/SearchMotoBreadcrumbsHondaCb400",
                        "/motorcycle/honda/cb_400/8131894731002395272/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(reviewMock,
                breadcrumbsMock,
                "reviews/CommentsReview").post();

        urlSteps.testing().path(REVIEW).path(category).path(path).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Обложка отзыва")
    public void shouldSeeCover() {
        basePageSteps.setNarrowWindowSize(8000);
        basePageSteps.onReviewPage().cover().counter().click();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().cover());

        urlSteps.setProduction().open();
        basePageSteps.onReviewPage().cover().counter().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().cover());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Содержимое отзыва")
    public void shouldSeeContent() {
        basePageSteps.setNarrowWindowSize(8000); //TODO Разделить эту портянку на блоки, в отчете целиком ее смотреть сложно
        basePageSteps.onReviewPage().cover().counter().click();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().content());

        urlSteps.setProduction().open();
        basePageSteps.onReviewPage().cover().counter().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().content());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке N комментариев")
    @Category({Regression.class})
    public void shouldClickCommentsButton() {
        basePageSteps.onReviewPage().cover().commentsButton().click();
        basePageSteps.onReviewPage().stickyHeader().waitUntil(isDisplayed());
    }
}
