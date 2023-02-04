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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MODELS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - выбор модели")
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ModelsTest {

    private static final String MARK = "Toyota";
    private static final String MODEL = "Corolla";

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsToyota").post();

        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODELS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Популярные модели")
    public void shouldSeePopularModels() {
        basePageSteps.onMarksAndModelsPage().popularModels().should(hasText("Любая модель\nПопулярные модели\nAuris\n" +
                "Avensis\nCamry\nCarina E\nCorolla\nFortuner\nHighlander\nHilux\nLand Cruiser\nLand Cruiser Prado\n" +
                "Prius\nRAV4"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Все модели")
    public void shouldSeeAllModels() {
        basePageSteps.onMarksAndModelsPage().allModels().should(hasText("Любая модель\n2\n2000GT\n4\n4Runner\nA\n" +
                "Allex\nAllion\nAlphard\nAltezza\nAqua\nAristo\nAurion\nAuris\nAvalon\nAvanza\nAvensis\nAvensis Verso\n" +
                "Aygo\nB\nbB\nBelta\nBlade\nBlizzard\nBrevis\nC\nC-HR\nCaldina\nCami\nCamry\nCamry Solara\nCarina\n" +
                "Carina E\nCarina ED\nCavalier\nCelica\nCelsior\nCentury\nChaser\nClassic\nComfort\nCOMS\nCorolla\n" +
                "Corolla II\nCorolla Rumion\nCorolla Spacio\nCorolla Verso\nCorona\nCorona EXiV\nCorsa\nCressida\n" +
                "Cresta\nCrown\nCrown Majesta\nCurren\nCynos\nD\nDuet\nE\nEcho\nEsquire\nEstima\nEtios\nF\nFJ Cruiser\n" +
                "Fortuner\nFunCargo\nG\nGaia\nGrand HiAce\nGranvia\nGT86\nH\nHarrier\nHiAce\nHighlander\nHilux\n" +
                "Hilux Surf\nI\nInnova\nIpsum\niQ\nISis\nIst\nK\nKluger\nL\nLand Cruiser\nLand Cruiser Prado\n" +
                "Lite Ace\nM\nMark II\nMark X\nMark X ZiO\nMasterAce Surf\nMatrix\nMega Cruiser\nMirai\nModel F\n" +
                "MR-S\nMR2\nN\nNadia\nNoah\nO\nOpa\nOrigin\nP\nPaseo\nPasso\nPasso Sette\nPicnic\nPixis Epoch\n" +
                "Pixis Joy\nPixis Mega\nPixis Space\nPixis Van\nPlatz\nPorte\nPremio\nPrevia\nPrius\nPrius Alpha\n" +
                "Prius c\nPrius v (+)\nProAce\nProbox\nProgres\nPronard\nPublica\nR\nRactis\nRaize\nRaum\nRAV4\n" +
                "Regius\nRegiusAce\nRush\nS\nSai\nScepter\nSequoia\nSera\nSienna\nSienta\nSoarer\nSoluna\nSpade\n" +
                "Sparky\nSports 800\nSprinter\nSprinter Carib\nSprinter Marino\nSprinter Trueno\nStarlet\nSucceed\n" +
                "Supra\nT\nTacoma\nTank\nTercel\nTouring HiAce\nTown Ace\nTundra\nU\nUrban Cruiser\nV\nVanguard\n" +
                "Vellfire\nVenza\nVerossa\nVerso\nVerso-S\nVios\nVista\nVitz\nVoltz\nVoxy\nW\nWiLL\nWiLL Cypha\n" +
                "Windom\nWish\nY\nYaris\nYaris Verso"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поиск модели")
    public void shouldSearchModel() {
        basePageSteps.onMarksAndModelsPage().searchInput().should(isDisplayed()).sendKeys("a");
        basePageSteps.onMarksAndModelsPage().allModels().waitUntil(hasText("A\nAllex\nAllion\nAlphard\nAltezza\nAqua\n" +
                "Aristo\nAurion\nAuris\nAvalon\nAvanza\nAvensis\nAvensis Verso\nAygo\nL\nLite Ace\nP\nPrius Alpha\nT\n" +
                "Town Ace"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор модели")
    @Category({Regression.class})
    public void shouldSelectModel() {
        mockRule.with("desktop/SearchCarsBreadcrumbsToyotaCorolla",
                "desktop/ProxySearcher").update();

        basePageSteps.onMarksAndModelsPage().model(MODEL).should(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path("/")
                .shouldNotSeeDiff();
    }
}
