package ru.yandex.arenda.costcalculator.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.CalculatorSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static ru.yandex.arenda.constants.UriPath.DVUHKOMNATNAYA;
import static ru.yandex.arenda.constants.UriPath.KALKULATOR_ARENDY;
import static ru.yandex.arenda.element.estimatecalculator.CallbackPopup.CALL_ME;
import static ru.yandex.arenda.element.estimatecalculator.CallbackPopup.INPUT_1_ID;
import static ru.yandex.arenda.element.estimatecalculator.PriceContainer.RENT_WITH_BUTTON;
import static ru.yandex.arenda.pages.CalculatorCostPage.ADDRESS_ID;
import static ru.yandex.arenda.pages.CalculatorCostPage.AREA_ID;
import static ru.yandex.arenda.pages.CalculatorCostPage.DONE_BUTTON;
import static ru.yandex.arenda.pages.CalculatorCostPage.FLOOR_ID;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_ADDRESS;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_AREA;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_FLOOR;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_RENOVATION;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_RENOVATION_VALUE;
import static ru.yandex.arenda.steps.CalculatorSteps.TEST_ROOMS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@Link("https://st.yandex-team.ru/VERTISTEST-2038")
@DisplayName("Калькулятор оценки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class CalculatorPageFilterTest {

    private static final String TEST_PHONE = "79000000000";
    private static final String EMPTY = "";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CalculatorSteps calculatorSteps;

    @Before
    public void before() {
        urlSteps.testing().path(KALKULATOR_ARENDY).open();
        calculatorSteps.setSpbCookie();
        calculatorSteps.refresh();
        calculatorSteps.fillTestCalculator(TEST_ADDRESS, TEST_ROOMS, TEST_AREA, TEST_FLOOR, TEST_RENOVATION);
        calculatorSteps.onCalculatorCostPage().button(DONE_BUTTON).click();
        calculatorSteps.shouldSeeTestCalculatorUrl(DVUHKOMNATNAYA, TEST_ADDRESS, TEST_AREA, TEST_FLOOR, TEST_RENOVATION_VALUE);

    }

    @Test
    @DisplayName("Заполнение калькулятора -> полный цикл")
    public void shouldSeeFullFillingCalculator() {

        calculatorSteps.onCalculatorCostPage().priceContainer().button(RENT_WITH_BUTTON).click();

        calculatorSteps.onCalculatorCostPage().callbackPopup().inputId(INPUT_1_ID).sendKeys(TEST_PHONE);
        calculatorSteps.onCalculatorCostPage().callbackPopup().button(CALL_ME).click();
        calculatorSteps.onCalculatorCostPage().successToast();
    }

    @Test
    @DisplayName("Заполнение калькулятора -> клик «Оценить другую»")
    public void shouldSeeEmptyForm() {
        calculatorSteps.onCalculatorCostPage().priceContainer().estimateOther().click();

        calculatorSteps.onCalculatorCostPage().inputId(ADDRESS_ID).should(hasValue(EMPTY));
        calculatorSteps.onCalculatorCostPage().inputId(AREA_ID).should(hasValue(EMPTY));
        calculatorSteps.onCalculatorCostPage().inputId(FLOOR_ID).should(hasValue(EMPTY));
    }
}
