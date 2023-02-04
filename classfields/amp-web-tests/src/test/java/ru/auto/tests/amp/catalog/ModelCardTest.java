package ru.auto.tests.amp.catalog;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.FILTERS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - карточка модели")
@Feature(AutoruFeatures.AMP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class ModelCardTest {

    private static final String MARK = "vaz";
    private static final String MODEL = "granta";
    private static final String LAST_GENERATION = "21377296";
    private static final String OTHER_GENERATION = "I";
    private static final String OTHER_GENERATION_ID = "7684102";
    private static final String GENERATION_USED = "21377296";
    private static final String LAST_BODY = "21377430";
    private static final String BODY = "21575582";
    private static final String BODY_USED = "21575582";
    private static final String GENERATION_NEW = "21377296";
    private static final String BODY_NEW = "21575582";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "amp/SearchVazGranta",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        urlSteps.testing().path(AMP).path(CATALOG).path(CARS).path(MARK).path(MODEL).path("/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор поколения в фильтре")
    public void shouldSelectGeneration() {
        basePageSteps.selectOption(basePageSteps.onCatalogModelPage().filter()
                .select("Выбрать поколение"), OTHER_GENERATION);
        urlSteps.path(OTHER_GENERATION_ID).path("/").ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Все параметры»")
    public void shouldClickAllParamsButton() {
        basePageSteps.onCatalogModelPage().filter().allParamsButton().should(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(FILTERS).ignoreParam("_gl")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Галерея")
    public void shouldSeeGallery() {
        basePageSteps.setWindowMaxHeight();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogModelPage().gallery());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogModelPage().gallery());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по последнему поколению")
    public void shouldClickLastGeneration() {
        basePageSteps.onCatalogModelPage().gallery().waitUntil(isDisplayed()).click();
        urlSteps.path(LAST_GENERATION).path(LAST_BODY).path("/").ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение сниппета кузова")
    public void shouldSeeBodySnippet() {
        basePageSteps.setWindowMaxHeight();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogModelPage().getBody(0).image());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogModelPage().getBody(0).image());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кузову")
    @Category({Regression.class})
    public void shouldClickBody() {
        basePageSteps.onCatalogModelPage().getBody(0).waitUntil(isDisplayed()).click();
        urlSteps.path(LAST_GENERATION).path(BODY).path("/").ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке на новые объявления кузова")
    @Category({Regression.class})
    public void shouldClickBodyNewSalesUrl() {
        basePageSteps.onCatalogModelPage().getBody(0).newSalesUrl().hover().click();
        urlSteps.testing().path(MOSKVA).path(AMP).path(CARS).path(MARK).path(MODEL).path(GENERATION_NEW).path(BODY_NEW).path(NEW)
                .ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке на б/у объявления кузова")
    @Category({Regression.class})
    public void shouldClickBodyUsedSalesUrl() {
        basePageSteps.onCatalogModelPage().getBody(0).usedSalesUrl().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(AMP).path(CARS).path(MARK).path(MODEL).path(GENERATION_USED).path(BODY_USED)
                .path(USED).path("/").ignoreParam("_gl").shouldNotSeeDiff();
    }
}
