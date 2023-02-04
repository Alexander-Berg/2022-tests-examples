package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithRadioButton extends VertisElement {

    @Name("Радио-кнопка «{{ text }}»")
    @FindBy(".//label[contains(@class, 'Radio_type_radio') and .//span[.= '{{ text }}']] | " +
            ".//div[@class = 'FormSection__ListItem' and .//div[@class = 'FormSection__ListItemText' " +
            "and .= '{{ text }}']] | " +
            ".//label[contains(@class, 'Radio_type_button') and .//span[.= '{{ text }}']] |" +
            ".//label[contains(@class, 'radio_type_button') and .//span[.= '{{ text }}']] | " +
            ".//button[contains(@class, 'button_togglable_radio') and .//div[.= '{{ text }}']] |" +
            ".//label[contains(@class, 'radio') and .//span[.= '{{ text }}']] |" +
            ".//label[contains(@class, 'Radio') and .//span[.= '{{ text }}']]")
    VertisElement radioButton(@Param("text") String Text);

    @Name("Радио-кнопка, в названии которой есть «{{ text }}»")
    @FindBy(".//label[contains(@class, 'radio_type_button') and .//span[contains(., '{{ text }}')]] |" +
            ".//div[@class = 'EvaluationResultGroupButtons__mobile-item' and contains(., '{{ text }}')] |" +
            ".//label[contains(@class, 'Radio_type_radio') and .//span[contains(., '{{ text }}')]]")
    VertisElement radioButtonContains(@Param("text") String Text);

    @Name("Выбранная радио-кнопка «{{ text }}»")
    @FindBy(".//label[contains(@class, 'Radio_type_button') and contains(@class, 'Radio_checked') " +
            "and .//span[.= '{{ text }}']] | " +
            ".//label[contains(@class, 'radio_checked') and .//span[.= '{{ text }}']]")
    VertisElement radioButtonSelected(@Param("text") String Text);
}
