package ru.yandex.arenda.costcalculator.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.CalculatorSteps;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static ru.yandex.arenda.constants.UriPath.KALKULATOR_ARENDY;
import static ru.yandex.arenda.constants.UriPath.ODNOKOMNATNAYA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Link("https://st.yandex-team.ru/VERTISTEST-2038")
@DisplayName("Калькулятор оценки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class RoomsFooterStudioTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CalculatorSteps calculatorSteps;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @DisplayName("Переход по ссылкам комнатности футера - Студия")
    public void shouldSeePassFromFooterStudio() {
        urlSteps.testing().path(KALKULATOR_ARENDY).path(ODNOKOMNATNAYA).open();
        calculatorSteps.onCalculatorCostPage().footer().link("Студия").click();
        calculatorSteps.waitUntilSeeTabsCountAndSwitch(2);
        urlSteps.testing().path(KALKULATOR_ARENDY).shouldNotDiffWithWebDriverUrl();
        calculatorSteps.onCalculatorCostPage().option("Студия").should(hasAttribute("selected", "true"));
    }

    @Test
    @DisplayName("Студия скриншот")
    public void shouldSeePassFromFooterStudioScreenshot() {
        compareSteps.resize(1600, 3000);
        urlSteps.testing().path(KALKULATOR_ARENDY).open();
        Screenshot testing = compareSteps.takeScreenshot(calculatorSteps.onCalculatorCostPage().root());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(calculatorSteps.onCalculatorCostPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
