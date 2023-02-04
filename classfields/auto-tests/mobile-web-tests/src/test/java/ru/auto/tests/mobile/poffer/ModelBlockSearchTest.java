package ru.auto.tests.mobile.poffer;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.PofferSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.poffer.beta.BetaModelBlock.MODEL;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.REFERENCE_CATALOG_CARS_SUGGEST_PATH;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поиск в блоке моделей")
@Epic(BETA_POFFER)
@Feature("Блок моделей")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ModelBlockSearchTest {

    private static final String REGION_ID = "225";
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

    @Parameterized.Parameter
    public String searchText;

    @Parameterized.Parameter(1)
    public String modelName;

    @Parameterized.Parameter(2)
    public String mock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<String[]> getParameters() {
        return asList(new String[][]{
                {"Priora", "Priora", "poffer/beta/ReferenceCatalogCarsSuggestLadaPriora"},
                {"приора", "Priora", "poffer/beta/ReferenceCatalogCarsSuggestLadaPriora"},
                {"ghbjhf", "Priora", "poffer/beta/ReferenceCatalogCarsSuggestLadaPriora"},
                {"Ока", "1111 Ока", "poffer/beta/ReferenceCatalogCarsSuggestLada1111"},
                {"jrf", "1111 Ока", "poffer/beta/ReferenceCatalogCarsSuggestLada1111"},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("poffer/beta/UserDraftCarsWithSelectedMark"),

                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS_SUGGEST_PATH)
                        .withRequestQuery(
                                query().setMark(MARK_CODE).setRid(REGION_ID))
                        .withResponseBody("poffer/beta/ReferenceCatalogCarsSuggestLadaResponse"),

                stub(mock)
        ).create();

        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ищем модель в списке")
    public void shouldSearchMark() {
        pofferSteps.onPofferPage().modelBlock().waitUntil(isDisplayed());
        pofferSteps.onPofferPage().modelBlock().input(MODEL, searchText);
        pofferSteps.onPofferPage().modelBlock().modelsList().waitUntil(hasSize(1));

        pofferSteps.onPofferPage().modelBlock().model(modelName).click();

        pofferSteps.onPofferPage().yearBlock().should(isDisplayed());
        pofferSteps.onPofferPage().yearBlock().yearsList().should(hasSize(greaterThan(0)));
    }

}
