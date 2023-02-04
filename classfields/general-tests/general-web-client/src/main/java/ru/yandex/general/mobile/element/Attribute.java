package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Attribute extends VertisElement, Input, Checkbox {

    @Name("Текст в инпуте селекта/мультиселекта")
    @FindBy(".//div[contains(@class, 'SelectButton__buttonText')]")
    VertisElement inputText();

    @Name("Кнопка очистки в инпуте селекта/мультиселекта")
    @FindBy(".//*[contains(@clip-path, 'Cross')]/..")
    VertisElement clearButton();

}
