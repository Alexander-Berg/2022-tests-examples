package ru.auto.tests.mobile.poffer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.mobile.step.PofferSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.poffer.beta.BetaMarkBlock.ALL_MARKS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.REFERENCE_CATALOG_CARS_SUGGEST_PATH;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок марок")
@Epic(BETA_POFFER)
@Feature("Блок марок")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class MarkBlockTest {

    private static final String MARK_CODE = "VAZ";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("poffer/beta/UserDraftCarsEmpty"),
                stub("poffer/beta/ReferenceCatalogCarsSuggestRid225")
        ).create();

        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).open();
        pofferSteps.onPofferPage().popup().closeIcon().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем марку из списка популярных")
    public void shouldSelectMarkFromPopularMarksList() {
        mockRule.setStubs(
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS_SUGGEST_PATH)
                        .withRequestQuery(
                                query().setMark(MARK_CODE))
                        .withResponseBody("poffer/beta/ReferenceCatalogCarsSuggestLadaResponse")
        ).update();

        pofferSteps.onPofferPage().markBlock().mark("LADA (ВАЗ)").click();

        pofferSteps.onPofferPage().modelBlock().should(isDisplayed())
                .should(hasText("Модель\n1111 Ока\n2101\n2104\n2105\n2106\n2107\n2109\n21099\n2110\n2111\n" +
                        "2112\n2113\n2114\n2115\n2121 (4x4)\n2131 (4x4)\nGranta\nKalina\nLargus\nNiva\n" +
                        "Niva Legend\nPriora\nVesta\nВсе модели"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем марку из списка всех марок")
    public void shouldSelectMarkFromAllMarksList() {
        mockRule.setStubs(stub("poffer/beta/ReferenceCatalogCarsSuggestLifan")).update();

        pofferSteps.onPofferPage().markBlock().button(ALL_MARKS).click();
        pofferSteps.waitSomething(2, TimeUnit.SECONDS);

        pofferSteps.onPofferPage().markBlock().mark("Lifan").waitUntil(isDisplayed()).click();

        pofferSteps.onPofferPage().modelBlock().should(isDisplayed())
                .should(hasText("Модель\n650 EV\nBreez (520)\nCebrium (720)\nCelliya (530)\nMurman (820)\n" +
                        "Myway\nSmily\nSolano\nX50\nX60\nX70"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны видеть список популярных марок")
    public void shouldSeePopularMarksList() {
        pofferSteps.onPofferPage().markBlock().should(hasText("Марка\nLADA (ВАЗ)\nAudi\nBMW\nHyundai\n" +
                "Kia\nMercedes-Benz\nNissan\nRenault\nToyota\nSkoda\nVolkswagen\nВсе марки"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение списка со всеми марками")
    public void shouldOpenAllMarksList() {
        pofferSteps.onPofferPage().markBlock().button(ALL_MARKS).click();
        pofferSteps.waitSomething(1, TimeUnit.SECONDS);

        pofferSteps.onPofferPage().markBlock()
                .should(hasText("Марка\nAC\nAcura\nAdler\nAlfa Romeo\nAlpina\nAlpine\nAM General\nAMC\n" +
                        "Apal\nAriel\nAro\nAsia\nAston Martin\nAuburn\nAudi\nAurus\nAustin\nAustin " +
                        "Healey\nAutobianchi\nBAIC\nBajaj\nBaltijas Dzips\nBentley\nBertone\nBilenkin\n" +
                        "Bio auto\nBitter\nBMW\nBorgward\nBrabus\nBrilliance\nBristol\nBufori\nBugatti\n" +
                        "Buick\nBYD\nByvin\nCadillac\nCallaway\nCarbodies\nCaterham\nChana\nChangan\n" +
                        "ChangFeng\nChanghe\nChery\nChevrolet\nChrysler\nCiimo\nCitroen\nCizeta\nCoggiola\n" +
                        "Cord\nCupra\nDacia\nDadi\nDaewoo\nDaihatsu\nDaimler\nDallara\nDatsun\nDe Tomaso\n" +
                        "Deco Rides\nDelage\nDeLorean\nDerways\nDeSoto\nDKW\nDodge\nDongFeng\nDoninvest\n" +
                        "Donkervoort\nDS\nDW Hower\nE-Car\nEagle\nEagle Cars\nEverus\nExcalibur\nEXEED\nFAW\n" +
                        "Ferrari\nFiat\nFisker\nFlanker\nFord\nFoton\nFSO\nFuqi\nGAC\nGeely\nGenesis\nGeo\nGMC\n" +
                        "Goggomobil\nGonow\nGordon\nGP\nGreat Wall\nHafei\nHaima\nHanomag\nHaval\nHawtai\nHeinkel\n" +
                        "Hennessey\nHindustan\nHispano-Suiza\nHolden\nHonda\nHongqi\nHorch\nHSV\nHuangHai\nHudson\n" +
                        "Hummer\nHyundai\nInfiniti\nInnocenti\nInternational\nInvicta\nIran Khodro\nIsdera\nIsuzu\n" +
                        "IVECO\nJAC\nJaguar\nJeep\nJensen\nJinbei\nJMC\nKia\nKoenigsegg\nKTM AG\nLADA (ВАЗ)\n" +
                        "Lamborghini\nLancia\nLand Rover\nLandwind\nLexus\nLiebao Motor\nLifan\nLigier\nLincoln\n" +
                        "LiXiang\nLogem\nLotus\nLTI\nLucid\nLuxgen\nMahindra\nMarcos\nMarlin\nMarussia\nMaruti\n" +
                        "Maserati\nMatra\nMaybach\nMazda\nMcLaren\nMega\nMercedes-Benz\nMercury\nMesserschmitt\n" +
                        "Metrocab\nMG\nMicrocar\nMinelli\nMINI\nMitsubishi\nMitsuoka\nMorgan\nMorris\nNash\nNio\n" +
                        "Nissan\nNoble\nOldsmobile\nOpel\nOsca\nPackard\nPagani\nPanoz\nPerodua\nPeugeot\nPGO\n" +
                        "Piaggio\nPierce-Arrow\nPlymouth\nPolestar\nPontiac\nPorsche\nPremier\nProton\nPUCH\nPuma\n" +
                        "Qoros\nQvale\nRAM\nRavon\nReliant\nRenaissance\nRenault\nRenault Samsung\nRezvani\nRinspeed" +
                        "\nRoewe\nRolls-Royce\nRonart\nRover\nSaab\nSaipa\nSaleen\nSantana\nSaturn\nScion\nSears\n" +
                        "SEAT\nShanghai Maple\nShuangHuan\nSimca\nSkoda\nSmart\nSoueast\nSpectre\nSpyker\nSsangYong" +
                        "\nSteyr\nStudebaker\nSubaru\nSuzuki\nTalbot\nTATA\nTatra\nTazzari\nTesla\nThink\nTianma\n" +
                        "Tianye\nTofas\nToyota\nTrabant\nTramontana\nTriumph\nTVR\nUltima\nVauxhall\nVector\n" +
                        "Venturi\nVolkswagen\nVolvo\nVortex\nVUHL\nW Motors\nWanderer\nWartburg\nWeltmeister\n" +
                        "Westfield\nWiesmann\nWillys\nXin Kai\nXpeng\nYulon\nZastava\nZenos\nZenvo\nZibar\nZotye\n" +
                        "ZX\nАвтокам\nГАЗ\nЗАЗ\nЗИЛ\nЗиС\nИЖ\nКанонир\nКомбат\nЛуАЗ\nМосквич\nСМЗ\nСпортивные авто " +
                        "и Реплики\nТагАЗ\nУАЗ\nЁ-мобиль"));
    }

}
