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
import ru.yandex.arenda.steps.MainSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.arenda.constants.UriPath.KALKULATOR_ARENDY;
import static ru.yandex.arenda.pages.CalculatorCostPage.DONE_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-2038")
@DisplayName("Калькулятор оценки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class FilterErrorTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MainSteps mainSteps;

    @Test
    @DisplayName("НЕ заполнение калькулятора -> видим ошибки")
    public void shouldSeeCalculatorErrors() {
        urlSteps.testing().path(KALKULATOR_ARENDY).open();
        mainSteps.onCalculatorCostPage().button(DONE_BUTTON).click();
        mainSteps.onCalculatorCostPage().inputError("Укажите адрес").should(isDisplayed());
        mainSteps.onCalculatorCostPage().inputError("Укажите площадь").should(isDisplayed());
        mainSteps.onCalculatorCostPage().inputError("Укажите этаж").should(isDisplayed());
        mainSteps.onCalculatorCostPage().inputError("Укажите тип ремонта").should(isDisplayed());
        mainSteps.onCalculatorCostPage().priceContainer().should(not(isDisplayed()));
    }
}
