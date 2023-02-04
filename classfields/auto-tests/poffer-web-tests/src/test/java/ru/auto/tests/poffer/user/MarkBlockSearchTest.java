package ru.auto.tests.poffer.user;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.poffer.beta.BetaMarkBlock.ALL_MARKS;
import static ru.auto.tests.desktop.element.poffer.beta.BetaMarkBlock.MARK;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поиск в блоке марок")
@Feature(BETA_POFFER)
@Story("Частник")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MarkBlockSearchTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String searchText;

    @Parameterized.Parameter(1)
    public String markName;

    @Parameterized.Parameter(2)
    public String mockPath;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<String[]> getParameters() {
        return asList(new String[][] {
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

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ищем марку в списке популярных")
    public void shouldSearchMarkInPopularList() {
        pofferSteps.onBetaPofferPage().markBlock().input(MARK, searchText);
        pofferSteps.onBetaPofferPage().markBlock().marksList().waitUntil(hasSize(1));

        pofferSteps.onBetaPofferPage().markBlock().mark(markName).click();

        pofferSteps.onBetaPofferPage().modelBlock().should(isDisplayed());
        pofferSteps.onBetaPofferPage().modelBlock().modelsList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ищем марку в попапе со всеми марками")
    public void shouldSearchMarkInAllMarksList() {
        pofferSteps.onBetaPofferPage().markBlock().button(ALL_MARKS).hover().click();
        pofferSteps.onBetaPofferPage().allMarksPopup().waitUntil(isDisplayed());

        pofferSteps.onBetaPofferPage().allMarksPopup().input(MARK, searchText);
        pofferSteps.onBetaPofferPage().allMarksPopup().marksList().waitUntil(hasSize(1));

        pofferSteps.onBetaPofferPage().allMarksPopup().button(markName).click();

        pofferSteps.onBetaPofferPage().modelBlock().should(isDisplayed());
        pofferSteps.onBetaPofferPage().modelBlock().modelsList().should(hasSize(greaterThan(0)));
    }
}
