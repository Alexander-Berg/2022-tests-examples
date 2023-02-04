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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.poffer.beta.BetaMarkBlock.MARK;
import static ru.auto.tests.desktop.element.poffer.beta.BetaModelBlock.ALL_MODELS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.REFERENCE_CATALOG_CARS_SUGGEST_PATH;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок моделей")
@Epic(BETA_POFFER)
@Feature("Блок моделей")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ModelBlockTest {

    private static final String REGION_ID = "225";
    private static final String MARK_CODE = "VAZ";
    private static final String MODEL = "Priora";

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
                stub("poffer/beta/UserDraftCarsWithSelectedMark"),

                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS_SUGGEST_PATH)
                        .withRequestQuery(
                                query().setMark(MARK_CODE).setRid(REGION_ID))
                        .withResponseBody("poffer/beta/ReferenceCatalogCarsSuggestLadaResponse")
        ).create();

        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем модель из списка популярных")
    public void shouldSelectModelFromPopularModelsList() {
        mockRule.setStubs(stub("poffer/beta/ReferenceCatalogCarsSuggestLadaPriora")).update();

        pofferSteps.onPofferPage().modelBlock().model(MODEL).click();

        pofferSteps.onPofferPage().yearBlock().should(isDisplayed())
                .should(hasText("Год выпуска\n2018\n2017\n2016\n2015\n2014\n2013\n2012\n" +
                        "2011\n2010\n2009\n2008\n2007"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем модель из списка со всеми моделями")
    public void shouldSelectModelFromAllModelsList() {
        mockRule.setStubs(stub("poffer/beta/ReferenceCatalogCarsSuggestLadaPriora")).update();

        int popularListSize = pofferSteps.onPofferPage().modelBlock().modelsList().size();

        pofferSteps.onPofferPage().modelBlock().button(ALL_MODELS).hover().click();
        pofferSteps.onPofferPage().modelBlock().modelsList().waitUntil(hasSize(greaterThan(popularListSize)));

        pofferSteps.onPofferPage().modelBlock().button(MODEL).click();

        pofferSteps.onPofferPage().yearBlock().should(isDisplayed())
                .should(hasText("Год выпуска\n2018\n2017\n2016\n2015\n2014\n2013\n2012\n" +
                        "2011\n2010\n2009\n2008\n2007"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбираем другую марку")
    public void shouldChangeMark() {
        mockRule.setStubs(stub("poffer/beta/ReferenceCatalogCarsSuggestKia")).update();

        pofferSteps.onPofferPage().markBlock().clearInput(MARK);
        pofferSteps.onPofferPage().markBlock().waitUntil(hasText("Марка\nLADA (ВАЗ)\nAudi\nBMW\nHyundai\n" +
                "Kia\nMercedes-Benz\nNissan\nRenault\nToyota\nSkoda\nVolkswagen\nВсе марки"));

        pofferSteps.onPofferPage().markBlock().mark("Kia").click();

        pofferSteps.onPofferPage().modelBlock().should(isDisplayed())
                .should(hasText("Модель\nCarens\nCarnival\nCeed\nCerato\nK5\nK900\nMagentis\nMohave\nOptima\n" +
                        "Picanto\nQuoris\nRio\nSeltos\nSephia\nShuma\nSorento\nSoul\nSpectra\nSportage\nStinger\n" +
                        "Venga\nXCeed\nВсе модели"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны видеть список популярных моделей")
    public void shouldSeePopularModelsList() {
        pofferSteps.onPofferPage().modelBlock().should(hasText("Модель\n1111 Ока\n2101\n2104\n2105\n2106\n2107" +
                "\n2109\n21099\n2110\n2111\n2112\n2113\n2114\n2115\n2121 (4x4)\n2131 (4x4)\nGranta\nKalina\nLargus" +
                "\nNiva\nNiva Legend\nPriora\nVesta\nВсе модели"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны видеть список всех моделей")
    public void shouldSeeAllModelsList() {
        pofferSteps.onPofferPage().modelBlock().button(ALL_MODELS).click();

        pofferSteps.onPofferPage().modelBlock().should(hasText("Модель\n1111 Ока\n2101\n2102\n2103\n2104\n2105" +
                "\n2106\n2107\n2108\n2109\n21099\n2110\n2111\n2112\n2113\n2114\n2115\n2120 Надежда\n2121 (4x4)\n" +
                "2123\n2129\n2131 (4x4)\n2328\n2329\nEL Lada\nGranta\nKalina\nLargus\nNiva\nNiva Legend\nPriora\n" +
                "Revolution\nVesta\nXRAY\nСвернуть"));
    }

}
