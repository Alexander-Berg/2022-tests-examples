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
import ru.yandex.arenda.steps.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.arenda.constants.UriPath.DVUHKOMNATNAYA;
import static ru.yandex.arenda.constants.UriPath.KALKULATOR_ARENDY;
import static ru.yandex.arenda.constants.UriPath.MNOGOKOMNATNAYA;
import static ru.yandex.arenda.constants.UriPath.ODNOKOMNATNAYA;
import static ru.yandex.arenda.constants.UriPath.TRYOHKOMNATNAYA;
import static ru.yandex.arenda.pages.CalculatorCostPage.DONE_BUTTON;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_ADDRESS;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_AREA;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_FLOOR;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_RENOVATION;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_RENOVATION_VALUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-2038")
@DisplayName("Калькулятор оценки")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RoomsFilterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CalculatorSteps calculatorSteps;

    @Parameterized.Parameter
    public String rooms;

    @Parameterized.Parameter(1)
    public String roomsParam;

    @Parameterized.Parameters(name = "для адреса {0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
//добавить потом для студий
//                {"Студия", СТУДИЯ_ПУТЬ},
                {"Комнат: 1", ODNOKOMNATNAYA},
                {"Комнат: 2", DVUHKOMNATNAYA},
                {"Комнат: 3", TRYOHKOMNATNAYA},
                {"Комнат: 4+", MNOGOKOMNATNAYA}
        });
    }

    @Test
    @DisplayName("Заполнение калькулятора -> разная комнатность")
    public void shouldSeeFullFillingForRooms() {
        urlSteps.testing().path(KALKULATOR_ARENDY).open();
        calculatorSteps.fillTestCalculator(TEST_ADDRESS, rooms, TEST_AREA, TEST_FLOOR, TEST_RENOVATION);
        calculatorSteps.onCalculatorCostPage().button(DONE_BUTTON).click();
        calculatorSteps.onCalculatorCostPage().priceContainer().waitUntil(isDisplayed());
        calculatorSteps.shouldSeeTestCalculatorUrl(roomsParam, TEST_ADDRESS, TEST_AREA, TEST_FLOOR,
                TEST_RENOVATION_VALUE);
    }
}
