package ru.auto.tests.mobile.catalog;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.COOKIESYNC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@DisplayName("Каталог - карточка кузова")
@Feature(AutoruFeatures.CATALOG)
public class BodyCardTest {

    private static final String MARK = "vaz";
    private static final String MODEL = "largus";
    private static final String GENERATION_ID = "22749611";
    private static final String BODY_ID = "22749668";
    private static final String COMPLECTATION = "1.6 MT 90 л.c. – Comfort Light 7 мест";
    private static final String COMPLECTATION_ID = "22749668_22957961_22749670";

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
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор комплектации в фильтре")
    @Category({Regression.class})
    public void shouldSelectComplectation() {
        basePageSteps.onCatalogBodyPage().filter().select("Выбрать комплектацию").should(isDisplayed()).click();
        basePageSteps.onCatalogBodyPage().dropdown().item(COMPLECTATION).waitUntil(isDisplayed()).click();

        urlSteps.path(SPECIFICATIONS).path(COMPLECTATION_ID).path(SLASH).ignoreParam(COOKIESYNC).shouldNotSeeDiff();
        basePageSteps.onCatalogBodyPage().complectationDescription().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Галерея")
    public void shouldSeeGallery() {
        basePageSteps.onCatalogBodyPage().gallery().should(isDisplayed());
        basePageSteps.onCatalogBodyPage().gallery().img()
                .should(hasAttribute("src",
                        format("%s/%s", urlSteps.getConfig().getAvatarsURI(),
                                "get-verba/1540742/2a00000180f633acc34bd6acd409f98b2bc4/cattouch")));
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

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Описание - клик по ссылке на новые объявления")
    public void shouldClickBodyNewSalesUrl() {
        basePageSteps.onCatalogBodyPage().description().newSalesUrl().should(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Описание - клик по ссылке на б/у объявления")
    public void shouldClickBodyUsedSalesUrl() {
        basePageSteps.onCatalogBodyPage().description().usedSalesUrl().should(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID).path(USED)
                .path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по баннеру «Смотреть все объявления»")
    public void shouldClickAllSalesBlock() {
        basePageSteps.onCatalogBodyPage().sales().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID).path(ALL)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
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
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Узнать подробнее» в блоке «Как дешеевеет это авто с возрастом»")
    public void shouldClickStatsMoreInfoUrl() {
        basePageSteps.onCatalogBodyPage().stats().moreInfoUrl().should(isDisplayed()).click();
        urlSteps.testing().path(STATS).path(CARS).path(MARK).path(MODEL).path(GENERATION_ID).path(BODY_ID)
                .path(SLASH).ignoreParam(COOKIESYNC).shouldNotSeeDiff();
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
        basePageSteps.onCatalogBodyPage().bodyComplectations().getComplectaion(0).should(isDisplayed()).click();

        urlSteps.path(SPECIFICATIONS).path(COMPLECTATION_ID).path(SLASH).shouldNotSeeDiff();
    }
}
