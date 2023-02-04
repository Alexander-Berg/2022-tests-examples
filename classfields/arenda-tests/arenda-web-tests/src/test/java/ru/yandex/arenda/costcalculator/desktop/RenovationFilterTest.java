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
import static ru.yandex.arenda.constants.UriPath.KALKULATOR_ARENDY;
import static ru.yandex.arenda.pages.CalculatorCostPage.COSMETIC_RENOVATION;
import static ru.yandex.arenda.pages.CalculatorCostPage.DESIGN_RENOVATION;
import static ru.yandex.arenda.pages.CalculatorCostPage.DONE_BUTTON;
import static ru.yandex.arenda.pages.CalculatorCostPage.EURO_RENOVATION;
import static ru.yandex.arenda.pages.CalculatorCostPage.GRANDMOM_RENOVATION;
import static ru.yandex.arenda.steps.CalculatorSteps.ADDRESS_PARAM;
import static ru.yandex.arenda.steps.CalculatorSteps.AREA_PARAM;
import static ru.yandex.arenda.steps.CalculatorSteps.FLOOR_PARAM;
import static ru.yandex.arenda.steps.CalculatorSteps.RENOVATION_PARAM;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_ADDRESS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-2038")
@DisplayName("Калькулятор оценки")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RenovationFilterTest {

    private static final String TEST_AREA = "9";
    private static final String TEST_FLOOR = "1";
    private static final String TEST_ROOMS = "Студия";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CalculatorSteps calculatorSteps;

    @Parameterized.Parameter
    public String renovation;

    @Parameterized.Parameter(1)
    public String renovationParam;

    @Parameterized.Parameters(name = "для адреса {0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {DESIGN_RENOVATION, "RENOVATION_DESIGNER_RENOVATIO"},
                {EURO_RENOVATION, "RENOVATION_EURO"},
                {COSMETIC_RENOVATION, "RENOVATION_COSMETIC_REQUIRE"},
                {GRANDMOM_RENOVATION, "RENOVATION_NEEDS_RENOVATIO"},
        });
    }

    @Test
    @DisplayName("Заполнение калькулятора -> разный ремонт")
    public void shouldSeeFullFillingForRenovation() {
        urlSteps.testing().path(KALKULATOR_ARENDY).open();
        calculatorSteps.fillTestCalculator(TEST_ADDRESS, TEST_ROOMS, TEST_AREA, TEST_FLOOR, renovation);
        calculatorSteps.onCalculatorCostPage().button(DONE_BUTTON).click();
        calculatorSteps.onCalculatorCostPage().priceContainer().waitUntil(isDisplayed());
        urlSteps.testing().path(KALKULATOR_ARENDY).queryParam("numberOfRooms", "0")
                .queryParam(ADDRESS_PARAM, TEST_ADDRESS)
                .queryParam(AREA_PARAM, TEST_AREA)
                .queryParam(FLOOR_PARAM, TEST_FLOOR).queryParam(RENOVATION_PARAM, renovationParam)
                .shouldNotDiffWithWebDriverUrl();
    }
}
