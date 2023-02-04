package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithCheckbox {

    @Name("Чекбокс «{{ text }}»")
    @FindBy(".//span[contains(@class, 'checkbox__text') and .= '{{ text }}'] |" +
            ".//span[contains(@class, 'Checkbox__text') and .= '{{ text }}']")
    VertisElement checkbox(@Param("text") String text);

    @Name("Чекбокс, в названии которого есть «{{ text }}»")
    @FindBy(".//span[contains(@class, 'Checkbox__text') and contains(., '{{ text }}')]")
    VertisElement checkboxContains(@Param("text") String text);

    @Name("Выбранный чекбокс «{{ text }}»")
    @FindBy(".//label[contains(@class, 'Checkbox_checked') and contains(.,'{{ text }}')]")
    VertisElement checkboxChecked(@Param("text") String text);

    @Name("Чекбокс")
    @FindBy(".//span[contains(@class, 'Checkbox__text')] | " +
            ".//span[contains(@class, 'checkbox__text')] | " +
            ".//label[contains(@class, 'Checkbox_type_checkbox')]")
    VertisElement checkbox();

    @Name("Выбранный чекбокс")
    @FindBy(".//label[contains(@class, 'Checkbox_checked')]")
    VertisElement checkboxChecked();
}