package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface RadioButton extends VertisElement {

    @Name("Радио-кнопка «{{ value }}»")
    @FindBy(".//label[contains(., '{{ value }}')][.//input[@type = 'radio']]")
    VertisElement radioButtonWithLabel(@Param("value") String value);

    @Name("Радио-кнопка")
    @FindBy(".//input[@type = 'radio']")
    VertisElement radioButton();

}
