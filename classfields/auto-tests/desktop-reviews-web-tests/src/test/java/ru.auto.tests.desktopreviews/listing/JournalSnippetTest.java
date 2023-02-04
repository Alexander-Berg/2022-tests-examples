package ru.auto.tests.desktopreviews.listing;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@DisplayName("Сниппет журнала в листинге отзывов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class JournalSnippetTest {

    private static final String MARK = "volkswagen";
    private static final String MODEL = "passat";
    private static final String NAMEPLATE = "20232305";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps steps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        steps.setNarrowWindowSize(5000);

        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(NAMEPLATE)
                .ignoreParam("sort").open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение сниппетов журнала в списке")
    public void shouldSeeJournalSnippets() {
        steps.onReviewsListingPage().firstJournalSnippet().should(isDisplayed());
        steps.onReviewsListingPage().secondJournalSnippet().should(isDisplayed());

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onReviewsListingPage().firstJournalSnippet());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onReviewsListingPage().firstJournalSnippet());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Клик по сниппету журнала")
    public void shouldClickJournalSnippet() {
        steps.onReviewsListingPage().firstJournalSnippet().waitUntil(isDisplayed()).click();
        urlSteps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path("/passatalldetails/")
                .addParam("utm_campaign", "reviews_test-drive")
                .addParam("utm_content", "passatalldetails")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .addParam("from", "reviews").shouldNotSeeDiff();
    }
}
