package ru.auto.tests.desktop.element.cabinet.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 19.04.18
 */
public interface FinancialWidget extends VertisElement {

    @Name("Поле ввода «Сумма пополнения»")
    @FindBy(".//div[contains(@class, 'TextInput__input')]//input")
    VertisElement rechargeAmount();

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//button[contains(@class, 'Button')][contains(., '{{ name }}')]")
    VertisElement button(@Param("name") String name);

}
