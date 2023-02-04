package ru.auto.tests.desktop.element.history;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface VinReportRepairCalculations extends VertisElement {

    @Name("Список расчётов стоимости ремонта")
    @FindBy(".//div[contains(@class, 'AccordionSection VinReportAccordion__item')]")
    ElementsCollection<VertisElement> repairCalculationsList();

    @Step("Получаем расчёт стоимости ремонта с индексом {i}")
    default VertisElement getRepairCalculation(int i) {
        return repairCalculationsList().should(hasSize(greaterThan(i))).get(i);
    }
}