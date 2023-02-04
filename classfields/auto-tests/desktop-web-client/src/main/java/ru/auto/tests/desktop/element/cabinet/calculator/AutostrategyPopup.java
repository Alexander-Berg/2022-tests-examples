package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface AutostrategyPopup extends VertisElement, WithInput, WithButton {

    String REDUCE = "Убавить";
    String APPEND = "Прибавить";
    String DISABLE = "Отключить";
    String PLACEHOLDER_TEMPLATE = "Не менее %d ₽";

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//button[contains(@title, '{{ name }}')]")
    VertisElement changeBid(@Param("name") String name);

    @Name("Кнопка «Применить»")
    @FindBy(".//button[contains(@class, '_applyButton')]")
    VertisElement applyButton();

}
