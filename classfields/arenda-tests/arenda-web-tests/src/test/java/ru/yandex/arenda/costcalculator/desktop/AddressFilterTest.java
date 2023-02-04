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
import static ru.yandex.arenda.pages.CalculatorCostPage.DONE_BUTTON;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_AREA;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_FLOOR;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_RENOVATION;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_RENOVATION_VALUE;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_ROOMS;

@Link("https://st.yandex-team.ru/VERTISTEST-2038")
@DisplayName("Калькулятор оценки")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddressFilterTest {

    private static final String MOSCOW_ADDRESS = "Россия, Москва, Гагаринский переулок, 31";
    private static final String SPB_ADDRESS = "Россия, Санкт-Петербург, улица Рубинштейна, 23";
    private static final String EKB_ADDRESS = "Россия, Свердловская область, Екатеринбург, проспект Космонавтов, 40";
    private static final String NOVOSIB_ADDRESS = "Россия, Новосибирск, улица Менделеева, 20";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CalculatorSteps calculatorSteps;

    @Parameterized.Parameter
    public String address;

    @Parameterized.Parameter(1)
    public String addressParam;

    @Parameterized.Parameters(name = "для адреса {0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {MOSCOW_ADDRESS, MOSCOW_ADDRESS},
                {SPB_ADDRESS, SPB_ADDRESS},
                {EKB_ADDRESS, EKB_ADDRESS},
                {NOVOSIB_ADDRESS, NOVOSIB_ADDRESS}
        });
    }

    @Test
    @DisplayName("Заполнение калькулятора -> разные адреса")
    public void shouldSeeFullFillingForAddress() {
        urlSteps.testing().path(KALKULATOR_ARENDY).open();
        calculatorSteps.fillTestCalculator(address, TEST_ROOMS, TEST_AREA, TEST_FLOOR, TEST_RENOVATION);
        calculatorSteps.onCalculatorCostPage().button(DONE_BUTTON).click();
        calculatorSteps.shouldSeeTestCalculatorUrl(DVUHKOMNATNAYA, addressParam, TEST_AREA, TEST_FLOOR,
                TEST_RENOVATION_VALUE);
    }
}
