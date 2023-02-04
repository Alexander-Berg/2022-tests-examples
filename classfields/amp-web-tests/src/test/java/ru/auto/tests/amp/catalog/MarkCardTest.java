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
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.FILTERS;
import static ru.auto.tests.desktop.consts.Pages.MARKS;
import static ru.auto.tests.desktop.consts.Pages.MODELS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - карточка марки")
@Feature(AutoruFeatures.AMP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class MarkCardTest {

    private static final String MARK = "toyota";
    private static final String MODEL = "4runner";

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
                "amp/SearchToyota",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        urlSteps.testing().path(AMP).path(CATALOG).path(CARS).path(MARK).path("/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход к выбору марки")
    public void shouldSelectMark() {
        basePageSteps.onCatalogMarkPage().filter().selectMarkButton().click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARKS).ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход к выбору модели")
    public void shouldSelectModel() {
        basePageSteps.onCatalogMarkPage().filter().selectModelButton().should(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODELS).ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Все параметры»")
    public void shouldClickAllParamsButton() {
        basePageSteps.onCatalogMarkPage().filter().allParamsButton().should(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(FILTERS).ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Описание")
    public void shouldSeeDescription() {
        basePageSteps.onCatalogMarkPage().description()
                .should(hasText("Toyota – самая крупная автомобилестроительная корпорация Японии. Компания производит " +
                        "легковые и грузовые автомобили, а также автобусы. Бренды Toyota, Lexus, Hino, Daihatsu, " +
                        "Scion – все они являются детищем этой корпорации. В мае 2012 года мировая продажа Тойота " +
                        "превысила все ожидания и компания вырвалась на первое место по производству автомобилей, " +
                        "обогнав General Motors и Volkswagen. Купить Тойота – значит отдать предпочтение японскому " +
                        "качеству и надежности. Объявления Yaris, Corolla, Camry, RAV4 или Land Cruiser Prado являются " +
                        "одними из самых запрашиваемых на автомобильном рынке России.\nчитать дальше"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Подробное описание")
    public void shouldSeeMoreInfo() {
        basePageSteps.onCatalogMarkPage().description().moreButton().click();
        basePageSteps.onCatalogMarkPage().description().should(hasText("Toyota – самая крупная автомобилестроительная " +
                "корпорация Японии. Компания производит легковые и грузовые автомобили, а также автобусы. " +
                "Бренды Toyota, Lexus, Hino, Daihatsu, Scion – все они являются детищем этой корпорации. " +
                "В мае 2012 года мировая продажа Тойота превысила все ожидания и компания вырвалась на первое место " +
                "по производству автомобилей, обогнав General Motors и Volkswagen. Купить Тойота – значит отдать " +
                "предпочтение японскому качеству и надежности. Объявления Yaris, Corolla, Camry, RAV4 или Land Cruiser " +
                "Prado являются одними из самых запрашиваемых на автомобильном рынке России.\nскрыть"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по блоку «Смотреть все объявления»")
    public void shouldClickShowAllSalesBlock() {
        basePageSteps.onCatalogMarkPage().showAllBlock().click();
        urlSteps.testing().path(MOSKVA).path(AMP).path(CARS).path(MARK).path(ALL).ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение сниппета модели")
    public void shouldSeeModelSnippet() {
        basePageSteps.setWindowHeight(3000);

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogMarkPage().getModel(0));

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogMarkPage().getModel(0));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        basePageSteps.onCatalogMarkPage().getModel(1).waitUntil(isDisplayed()).click();
        urlSteps.testing().path(AMP).path(CATALOG).path(CARS).path(MARK).path(MODEL).path("/").ignoreParam("_gl")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке на объявления модели")
    public void shouldClickModelSalesUrl() {
        basePageSteps.onCatalogMarkPage().getModel(1).button("2 с пробегом").waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(AMP).path(CARS).path(MARK).path(MODEL).path(USED).path("/").ignoreParam("_gl")
                .shouldNotSeeDiff();
    }
}
