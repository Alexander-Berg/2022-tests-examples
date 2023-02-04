package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 05.12.18
 */
public interface Balance extends VertisElement {

    String ACCOMMODATION_IN_CATEGORIES_SUMMARY = "Размещение в\u00a0категориях"; //u00A0
    String SERVICES_IN_CATEGORIES_SUMMARY = "Услуги в\u00a0категориях";

    @Name("Сводка «{{ name }}»")
    @FindBy(".//table[contains(@class, 'CalculatorSummaryTable__table')]//tr[contains(., '{{ name }}')]")
    VertisElement summary(@Param("name") String name);

    @Name("Пополнить счёт")
    @FindBy(".//div[@class = 'CalculatorBalance__footerContainer']//button")
    VertisElement replenish();

    @FindBy(".//table[contains(@class, 'CalculatorSummaryTable__table')]//tr[contains(., 'Итог')]")
    VertisElement test();

    @FindBy(".//tr[contains(., '{{ blockName }}')]//*[contains(@class, 'CalculatorSummaryTableHeader__toggleIcon')]")
    VertisElement toggleIcon(@Param("blockName") String blockName);
}
