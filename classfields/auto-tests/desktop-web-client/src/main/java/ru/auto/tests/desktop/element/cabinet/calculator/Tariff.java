package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 18.12.18
 */
public interface Tariff extends VertisElement {

    @Name("Активировать тариф")
    @FindBy(".//span[contains(@class, 'CalculatorQuotaTableItem__buttonActivate')]")
    VertisElement activate();

    @Name("Отключить тариф")
    @FindBy(".//span[contains(@class, 'Link')][contains(., 'отключить')]")
    VertisElement turnOffTariff();

    @Name("Отменить")
    @FindBy(".//span[contains(@class, 'Link')][contains(., 'отменить')]")
    VertisElement cancel();
}
