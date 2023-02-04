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
import static ru.auto.tests.desktop.element.poffer.beta.BetaMarkBlock.MARK;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поиск в блоке марок")
@Epic(BETA_POFFER)
@Feature("Блок марок")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MarkBlockSearchTest {

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
    public String markName;

    @Parameterized.Parameter(2)
    public String mockPath;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<String[]> getParameters() {
        return asList(new String[][]{
                {"Москвич", "Москвич", "poffer/beta/ReferenceCatalogCarsSuggestMoscvich"},
                {"vjcrdbx", "Москвич", "poffer/beta/ReferenceCatalogCarsSuggestMoscvich"},
                {"Lifan", "Lifan", "poffer/beta/ReferenceCatalogCarsSuggestLifan"},
                {"лифан", "Lifan", "poffer/beta/ReferenceCatalogCarsSuggestLifan"},
                {"kbafy", "Lifan", "poffer/beta/ReferenceCatalogCarsSuggestLifan"},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("poffer/beta/UserDraftCarsEmpty"),
                stub("poffer/beta/ReferenceCatalogCarsSuggestRid225"),
                stub(mockPath)
        ).create();

        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).open();
        pofferSteps.onPofferPage().popup().closeIcon().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ищем марку")
    public void shouldSearchMark() {
        pofferSteps.onPofferPage().markBlock().input(MARK, searchText);
        pofferSteps.onPofferPage().markBlock().marksList().waitUntil(hasSize(1));

        pofferSteps.onPofferPage().markBlock().mark(markName).click();

        pofferSteps.onPofferPage().modelBlock().should(isDisplayed());
        pofferSteps.onPofferPage().modelBlock().modelsList().should(hasSize(greaterThan(0)));
    }

}
