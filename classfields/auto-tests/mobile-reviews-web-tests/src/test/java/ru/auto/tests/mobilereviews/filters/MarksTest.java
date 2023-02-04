package ru.auto.tests.mobilereviews.filters;

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
import pazone.ashot.Screenshot;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MARKS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Выбор марки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class MarksTest {

    private static final String MARK = "Toyota";
    private static final String IGNORE_ELEMENTS = "//div[@class = 'reference__counter']";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).path(MARKS).path(CARS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Все марки")
    public void shouldSeeAllMarks() {
        Screenshot testingScreenshot =
                screenshotSteps.getElementScreenshotIgnoreElements(basePageSteps.onMarksAndModelsPage().allMarks()
                        .should(isDisplayed()), newHashSet(IGNORE_ELEMENTS));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot =
                screenshotSteps.getElementScreenshotIgnoreElements(basePageSteps.onMarksAndModelsPage().allMarks()
                        .should(isDisplayed()), newHashSet(IGNORE_ELEMENTS));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Поиск марки")
    public void shouldSearchMark() {
        basePageSteps.onMarksAndModelsPage().searchInput().should(isDisplayed()).sendKeys("a");
        Screenshot testingScreenshot =
                screenshotSteps.getElementScreenshotIgnoreElements(basePageSteps.onMarksAndModelsPage().allMarks()
                        .waitUntil(isDisplayed()), newHashSet(IGNORE_ELEMENTS));

        urlSteps.setProduction().open();
        basePageSteps.onMarksAndModelsPage().searchInput().should(isDisplayed()).sendKeys("a");
        Screenshot productionScreenshot =
                screenshotSteps.getElementScreenshotIgnoreElements(basePageSteps.onMarksAndModelsPage().allMarks()
                        .waitUntil(isDisplayed()), newHashSet(IGNORE_ELEMENTS));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        basePageSteps.onMarksAndModelsPage().mark(MARK).should(isDisplayed()).click();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK.toLowerCase()).path("/")
                .shouldNotSeeDiff();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(greaterThan(0)));
    }
}
