package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithRadioButton extends VertisElement {

    @Name("Радио-кнопка «{{ text }}»")
    @FindBy(".//label[contains(@class, 'Radio_type_button') and .//span[.= '{{ text }}']] | " +
            ".//label[contains(@class, 'Radio_type_radio') and .//span[.= '{{ text }}']]")
    VertisElement radioButton(@Param("text") String Text);

    @Name("Радио-кнопка, в названии которой есть «{{ text }}»")
    @FindBy(".//label[contains(@class, 'Radio_type_button') and .//span[contains(., '{{ text }}')]] | " +
            ".//label[contains(@class, 'Radio_type_radio') and .//span[contains(., '{{ text }}')]]")
    VertisElement radioButtonContains(@Param("text") String Text);
}