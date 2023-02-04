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
import static ru.auto.tests.desktop.consts.Pages.MARKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - выбор марки")
@Feature(AutoruFeatures.CATALOG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class MarksTest {

    private static final String MARK = "Toyota";

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213").post();

        urlSteps.testing().path(CATALOG).path(CARS).path(MARKS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Популярные марки")
    public void shouldSeePopularMarks() {
        basePageSteps.onMarksAndModelsPage().popularMarks().should(hasText("Любая марка\nПопулярные марки\nLADA (ВАЗ)\n" +
                "Audi\nBMW\nChery\nChevrolet\nCitroen\nDaewoo\nFord\nHonda\nHyundai\nInfiniti\nKia\nLand Rover\nLexus\n" +
                "Lifan\nMazda\nMercedes-Benz\nMitsubishi\nNissan\nOpel\nPeugeot\nRenault\nSkoda\nSsangYong\nSubaru\n" +
                "Suzuki\nToyota\nVolkswagen\nVolvo\nГАЗ"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Все марки")
    public void shouldSeeAllMarks() {
        basePageSteps.onMarksAndModelsPage().allMarks().should(hasText("Любая марка\nA\nAC\nAcura\nAdler\nAlfa Romeo\n" +
                "Alpina\nAlpine\nAM General\nAMC\nApal\nAriel\nAro\nAsia\nAston Martin\nAudi\nAurus\nAustin\n" +
                "Austin Healey\nAutobianchi\nB\nBAIC\nBajaj\nBaltijas Dzips\nBatmobile\nBentley\nBertone\nBilenkin\n" +
                "Bitter\nBMW\nBorgward\nBrabus\nBrilliance\nBristol\nBufori\nBugatti\nBuick\nBYD\nByvin\nC\nCadillac\n" +
                "Callaway\nCarbodies\nCaterham\nChana\nChangan\nChangFeng\nChanghe\nChery\nChevrolet\nChrysler\n" +
                "Citroen\nCizeta\nCoggiola\nCord\nD\nDacia\nDadi\nDaewoo\nDaihatsu\nDaimler\nDatsun\nDe Tomaso\n" +
                "Delage\nDeLorean\nDerways\nDeSoto\nDKW\nDodge\nDongFeng\nDoninvest\nDonkervoort\nDS\nDW Hower\n" +
                "E\nE-Car\nEagle\nEagle Cars\nExcalibur\nF\nFAW\nFerrari\nFiat\nFisker\nFlanker\nFord\nFoton\n" +
                "FSO\nFuqi\nG\nGAC\nGeely\nGenesis\nGeo\nGMC\nGonow\nGordon\nGP\nGreat Wall\nH\nHafei\nHaima\n" +
                "Hanomag\nHaval\nHawtai\nHindustan\nHispano-Suiza\nHolden\nHonda\nHorch\nHuangHai\nHudson\n" +
                "Hummer\nHyundai\nI\nInfiniti\nInnocenti\nInternational\nInvicta\nIran Khodro\nIsdera\n" +
                "Isuzu\nIVECO\nJ\nJAC\nJaguar\nJeep\nJensen\nJinbei\nJMC\nK\nKia\nKoenigsegg\nKTM AG\nL\nLADA (ВАЗ)\n" +
                "Lamborghini\nLancia\nLand Rover\nLandwind\nLexus\nLiebao Motor\nLifan\nLigier\nLincoln\nLogem\n" +
                "Lotus\nLTI\nLucid\nLuxgen\nM\nMahindra\nMarcos\nMarlin\nMarussia\nMaruti\nMaserati\nMaybach\nMazda\n" +
                "McLaren\nMega\nMercedes-Benz\nMercury\nMetrocab\nMG\nMicrocar\nMinelli\nMINI\nMitsubishi\nMitsuoka\n" +
                "Morgan\nMorris\nN\nNash\nNissan\nNoble\nO\nOldsmobile\nOpel\nOsca\nP\nPackard\nPagani\nPanoz\nPerodua\n" +
                "Peugeot\nPGO\nPiaggio\nPlymouth\nPontiac\nPorsche\nPremier\nProton\nPUCH\nPuma\nQ\nQoros\nQvale\nR\n" +
                "Rambler\nRavon\nReliant\nRenaissance\nRenault\nRenault Samsung\nRezvani\nRimac\nRinspeed\nRolls-Royce\n" +
                "Ronart\nRover\nS\nSaab\nSaipa\nSaleen\nSantana\nSaturn\nScion\nSEAT\nShanghai Maple\nShuangHuan\n" +
                "Simca\nSkoda\nSmart\nSoueast\nSpectre\nSpyker\nSsangYong\nSteyr\nStudebaker\nSubaru\nSuzuki\nT\n" +
                "Talbot\nTATA\nTatra\nTazzari\nTesla\nThink\nTianma\nTianye\nTofas\nToyota\nTrabant\nTramontana\n" +
                "Triumph\nTVR\nU\nUltima\nV\nVauxhall\nVector\nVenturi\nVolkswagen\nVolvo\nVortex\nW\nW Motors\n" +
                "Wanderer\nWartburg\nWestfield\nWiesmann\nWillys\nX\nXin Kai\nZ\nZastava\nZenos\nZenvo\nZibar\n" +
                "Zotye\nZX\nЁ\nЁ-мобиль\nА\nАвтокам\nГ\nГАЗ\nГоночный болид\nЗ\nЗАЗ\nЗИЛ\nЗиС\nИ\nИЖ\nК\nКанонир\n" +
                "Комбат\nЛ\nЛуАЗ\nМ\nМосквич\nС\nСМЗ\nТ\nТагАЗ\nУ\nУАЗ"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поиск марки")
    public void shouldSearchMark() {
        basePageSteps.onMarksAndModelsPage().searchInput().should(isDisplayed()).sendKeys("a");
        basePageSteps.onMarksAndModelsPage().allMarks().waitUntil(hasText("A\nAC\nAcura\nAdler\nAlfa Romeo\nAlpina\n" +
                "Alpine\nAM General\nAMC\nApal\nAriel\nAro\nAsia\nAston Martin\nAudi\nAurus\nAustin\nAustin Healey\n" +
                "Autobianchi\nK\nKTM AG"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор марки")
    @Category({Regression.class, Testing.class})
    public void shouldSelectMark() {
        basePageSteps.onMarksAndModelsPage().mark(MARK).should(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path("/").shouldNotSeeDiff();
    }
}
