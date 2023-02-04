package ru.auto.tests.mobilereviews.review;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import pazone.ashot.Screenshot;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@DisplayName("Страница отзыва")
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReviewTest {

    private final static String TEXT_CAR = "Оценка автора\n4,2\nВнешний вид\n5\nКомфорт\n5\nБезопасность\n5\nХодовые качества\n5\nНадёжность\n1";
    private final static String TEXT_MOTO = "Оценка автора\n4,0\nВнешний вид\n4\nКомфорт\n4\nБезопасность\n3\nХодовые качества\n4\nНадёжность\n5";
    private final static String TEXT_TRUCK = "Оценка автора\n1,2\nВнешний вид\n2\nКомфорт\n1\nБезопасность\n1\nХодовые качества\n1\nНадёжность\n1";

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
    public String mock;

    @Parameterized.Parameter(2)
    public String path;

    @Parameterized.Parameter(3)
    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "reviews/ReviewsAutoCars", "/uaz/patriot/2309645/4014660/", TEXT_CAR},
                {MOTO, "reviews/ReviewsAutoMoto", "/motorcycle/honda/cb_400/8131894731002395272/", TEXT_MOTO},
                {TRUCKS, "reviews/ReviewsAutoTrucks", "/truck/foton/aumark_10xx/4033391/", TEXT_TRUCK}});
    }

    @Before
    public void before() {
        mockRule.newMock().with(mock,
                "reviews/SearchTrucksBreadcrumbsFotonAumark10xx",
                "reviews/SearchMotoBreadcrumbsHondaCb400",
                "reviews/SearchCarsBreadcrumbsUazPatriot",
                "reviews/CommentsReview").post();

        urlSteps.testing().path(REVIEW).path(category).path(path).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Содержимое отзыва")
    public void shouldSeeContent() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().reviewContent());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onReviewPage().reviewContent());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение блока «Оценка автора»")
    public void shouldSeeAuthorBlock() {
        basePageSteps.onReviewPage().authorBlock().hover().waitUntil(isDisplayed());
        basePageSteps.onReviewPage().authorBlock().should(hasText(text));
    }

}
