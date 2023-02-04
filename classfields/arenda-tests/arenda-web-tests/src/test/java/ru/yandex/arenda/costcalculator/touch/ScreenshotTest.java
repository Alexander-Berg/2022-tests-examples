package ru.yandex.arenda.costcalculator.touch;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.arenda.module.ArendaTouchModule;
import ru.yandex.arenda.steps.CalculatorSteps;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static ru.yandex.arenda.constants.UriPath.KALKULATOR_ARENDY;

@Link("https://st.yandex-team.ru/VERTISTEST-2038")
@DisplayName("Калькулятор оценки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaTouchModule.class)
public class ScreenshotTest {

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
    @DisplayName("Тач. Скриншот")
    public void shouldSeePassFromFooterStudioScreenshot() {
        compareSteps.resize(375, 3500);
        urlSteps.testing().path(KALKULATOR_ARENDY).open();
        Screenshot testing = compareSteps.takeScreenshot(calculatorSteps.onCalculatorCostPage().root());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(calculatorSteps.onCalculatorCostPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
