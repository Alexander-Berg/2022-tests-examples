package ru.auto.tests.mobile.catalog;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
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
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class MarkCardTest {

    private static final String MARK = "toyota";
    private static final Integer MODELS_CNT = 34;
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

    @Before
    public void before() {
        mockRule.newMock().with("mobile/SearchToyota",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();


        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path("/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход к выбору марки")
    public void shouldSelectMark() {
        basePageSteps.onCatalogMarkPage().filter().selectMarkButton().should(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARKS).ignoreParam("cookiesync").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход к выбору модели")
    public void shouldSelectModel() {
        basePageSteps.onCatalogMarkPage().filter().selectModelButton().should(isDisplayed()).click();
        urlSteps.path(MODELS).ignoreParam("cookiesync").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Все параметры»")
    public void shouldClickAllParamsButton() {
        basePageSteps.onCatalogMarkPage().filter().allParamsButton().should(isDisplayed()).click();
        urlSteps.path(FILTERS).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
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
    @Category({Regression.class, Testing.class})
    @DisplayName("Подробное описание")
    public void shouldSeeMoreInfo() {
        basePageSteps.onCatalogMarkPage().description().moreButton().click();
        basePageSteps.onCatalogMarkPage().description().should(hasText("Toyota – самая крупная автомобилестроительная " +
                "корпорация Японии. Компания производит легковые и грузовые автомобили, а также автобусы. " +
                "Бренды Toyota, Lexus, Hino, Daihatsu, Scion – все они являются детищем этой корпорации. " +
                "В мае 2012 года мировая продажа Тойота превысила все ожидания и компания вырвалась на первое место " +
                "по производству автомобилей, обогнав General Motors и Volkswagen. Купить Тойота – значит отдать " +
                "предпочтение японскому качеству и надежности. Объявления Yaris, Corolla, Camry, RAV4 или Land Cruiser " +
                "Prado являются одними из самых запрашиваемых на автомобильном рынке России."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Issue("AUTORUFRONT-21571")
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по блоку «Смотреть все объявления»")
    public void shouldClickShowAllSalesBlock() {
        basePageSteps.onCatalogMarkPage().showAllBlock().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сниппета модели")
    public void shouldSeeModelSnippet() {
        basePageSteps.onCatalogMarkPage().getModel(0).should(hasText("1/6\nToyota 2000GT"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подгрузка моделей")
    public void shouldLoadMoreModels() {
        basePageSteps.onCatalogMarkPage().modelsList().waitUntil(hasSize(MODELS_CNT));
        basePageSteps.scrollDown(30000);
        basePageSteps.onCatalogMarkPage().modelsList().waitUntil(hasSize(MODELS_CNT * 2));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        basePageSteps.onCatalogMarkPage().getModel(1).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке на объявления модели")
    public void shouldClickModelSalesUrl() {
        basePageSteps.onCatalogMarkPage().getModel(1).salesUrl().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(USED).path("/").shouldNotSeeDiff();
    }
}
