package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 11.12.18
 */
public interface CalculatorCallsLimitsPopup extends VertisElement {

    String APPLY = "Применить";
    String NO_LIMITS = "Нет ограничений";

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//button[contains(., '{{ name }}')]")
    VertisElement button(@Param("name") String name);

    @Name("Сумма лимита")
    @FindBy(".//label//input")
    VertisElement input();

    @Name("Крестик очистки ввода")
    @FindBy(".//i[contains(@class, 'TextInput__clear_visible')]")
    VertisElement clearLimit();

}
