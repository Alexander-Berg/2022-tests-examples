package ru.yandex.arenda.costcalculator.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.CalculatorSteps;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.arenda.constants.UriPath.DVUHKOMNATNAYA;
import static ru.yandex.arenda.constants.UriPath.KALKULATOR_ARENDY;
import static ru.yandex.arenda.constants.UriPath.MNOGOKOMNATNAYA;
import static ru.yandex.arenda.constants.UriPath.ODNOKOMNATNAYA;
import static ru.yandex.arenda.constants.UriPath.TRYOHKOMNATNAYA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Link("https://st.yandex-team.ru/VERTISTEST-2038")
@DisplayName("Калькулятор оценки")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RoomsFooterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CalculatorSteps calculatorSteps;

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public String rooms;

    @Parameterized.Parameter(1)
    public String addressPath;

    @Parameterized.Parameter(2)
    public String addressOption;

    @Parameterized.Parameters(name = "для адреса {0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"1-комнатные", ODNOKOMNATNAYA, "Комнат: 1"},
                {"2-комнатные", DVUHKOMNATNAYA, "Комнат: 2"},
                {"3-комнатные", TRYOHKOMNATNAYA, "Комнат: 3"},
                {"4-комнатные", MNOGOKOMNATNAYA, "Комнат: 4+"}
        });
    }

    @Test
    @DisplayName("Переход по ссылкам комнатности футера")
    public void shouldSeePassFromFooter() {
        urlSteps.testing().path(KALKULATOR_ARENDY).open();
        calculatorSteps.onCalculatorCostPage().footer().link(rooms).click();
        calculatorSteps.waitUntilSeeTabsCountAndSwitch(2);
        urlSteps.testing().path(KALKULATOR_ARENDY).path(addressPath).shouldNotDiffWithWebDriverUrl();
        calculatorSteps.onCalculatorCostPage().option(addressOption).should(hasAttribute("selected", "true"));
    }

    @Test
    @DisplayName("Скриншот с комнатностью")
    public void shouldSeePassFromFooterScreenshot() {
        compareSteps.resize(1600, 3000);
        urlSteps.testing().path(KALKULATOR_ARENDY).path(addressPath).open();
        Screenshot testing = compareSteps.takeScreenshot(calculatorSteps.onCalculatorCostPage().root());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(calculatorSteps.onCalculatorCostPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
