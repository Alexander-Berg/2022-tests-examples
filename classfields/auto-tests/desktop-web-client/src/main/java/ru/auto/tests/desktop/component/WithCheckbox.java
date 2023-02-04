package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithCheckbox {

    String CHECKBOX_DISABLED = "Checkbox_disabled";

    @Name("Чекбокс «{{ text }}»")
    @FindBy(".//span[contains(@class, 'checkbox__text') and .= '{{ text }}'] |" +
            ".//span[contains(@class, 'Checkbox__text') and .= '{{ text }}'] | " +
            ".//label[contains(@class, 'checkbox') and .= '{{ text }}']")
    VertisElement checkbox(@Param("text") String text);

    @Name("Чекбокс, в названии которого есть «{{ text }}»")
    @FindBy(".//span[contains(@class, 'Checkbox__text') and contains(., '{{ text }}')] | " +
            ".//label[contains(@class, 'checkbox') and contains(., '{{ text }}')]")
    VertisElement checkboxContains(@Param("text") String text);

    @Name("Активированный чекбокс «{{ text }}»")
    @FindBy(".//label[contains(@class, 'Checkbox_checked') and contains(.,'{{ text }}')] | " +
            ".//label[contains(@class, 'checkbox_checked') and contains(.,'{{ text }}')]")
    VertisElement checkboxChecked(@Param("text") String text);

    @Name("Чекбокс")
    @FindBy(".//span[contains(@class, 'Checkbox__text')] | " +
            ".//span[contains(@class, 'checkbox__text')] | " +
            ".//span[contains(@class, 'Checkbox__box')]")
    VertisElement checkbox();
}
