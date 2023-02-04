package ru.auto.tests.amp.catalog;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
@DisplayName("Каталог - карточка кузова")
@Feature(AutoruFeatures.AMP)
public class BodyCardTest {

    private static final String MARK = "bmw";
    private static final String MODEL = "x5";
    private static final String GENERATION_ID = "21307931";
    private static final String BODY_ID = "21307996";
    private static final String COMPLECTATION = "2.0d AT 231 л.c. – xDrive25d Business";
    private static final String COMPLECTATION_ID = "21307996_21935248_21718431";
    private static final String FIRST_COMPLECTATION_ID = "21307996_21362827_21308175";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("amp/SearchBmwX3"),
                stub("amp/SearchCarsCountNew"),
                stub("amp/SearchCarsCountUsed"),
                stub("desktop/ProxyPublicApi"),
                stub("desktop/ProxySearcher")).create();

        urlSteps.testing().path(AMP).path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID)
                .open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор комплектации в фильтре")
    @Category({Regression.class})
    public void shouldSelectComplectation() {
        basePageSteps.selectOption(basePageSteps.onCatalogModelPage().filter()
                .select("Выбрать комплектацию"), COMPLECTATION);
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID)
                .path(SPECIFICATIONS).path(COMPLECTATION_ID).path("/").ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Галерея")
    public void shouldSeeGallery() {
        screenshotSteps.setWindowSizeForScreenshot();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogBodyPage().gallery());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogBodyPage().gallery());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Описание")
    public void shouldSeeDescription() {
        basePageSteps.setWindowMaxHeight();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogBodyPage().description());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogBodyPage().description());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Описание - клик по кнопке «Купить новый»")
    public void shouldClickBuyNewButton() {
        basePageSteps.onCatalogBodyPage().buyNewButton().should(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(AMP).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID).path(NEW)
                .ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Описание - клик по ссылке на новые объявления")
    public void shouldClickBodyNewSalesUrl() {
        basePageSteps.onCatalogBodyPage().description().newSalesUrl().should(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(AMP).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID).path(NEW)
                .ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Описание - клик по ссылке на б/у объявления")
    public void shouldClickBodyUsedSalesUrl() {
        basePageSteps.onCatalogBodyPage().description().usedSalesUrl().should(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(AMP).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID).path(USED)
                .path("/").ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по блоку «Смотреть все объявления»")
    public void shouldClickAllSalesBlock() {
        basePageSteps.onCatalogBodyPage().sales().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(AMP).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID).path(ALL)
                .ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Issue("AUTORUFRONT-20961")
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Блок «Как дешеевеет это авто с возрастом»")
    public void shouldSeeStats() {
        basePageSteps.setWindowMaxHeight();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogBodyPage().stats());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogBodyPage().stats());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Issue("AUTORUFRONT-20961")
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Узнать подробнее» в блоке «Как дешеевеет это авто с возрастом»")
    public void shouldClickStatsMoreInfoUrl() {
        basePageSteps.onCatalogBodyPage().stats().moreInfoUrl().should(isDisplayed()).click();
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID)
                .path("/").ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Комплектации")
    public void shouldSeeComplectations() {
        basePageSteps.setWindowMaxHeight();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogBodyPage().bodyComplectations());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogBodyPage().bodyComplectations());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по комплектации")
    public void shouldClickComplectationUrl() {
        basePageSteps.onCatalogBodyPage().bodyComplectations().complectationsList().should(hasSize(greaterThan(0)))
                .get(0).should(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID)
                .path(SPECIFICATIONS).path(FIRST_COMPLECTATION_ID).path("/").ignoreParam("_gl").shouldNotSeeDiff();
    }
}
