package ru.yandex.arenda.element.common;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Input extends AtlasWebElement {

    @Name("Инпут id=«{{ value }}»")
    @FindBy(".//input[@id='{{ value }}']")
    InputClearCross inputId(@Param("value") String value);

    @Name("Текстовое поле id=«{{ value }}»")
    @FindBy(".//textarea[@id='{{ value }}']")
    AtlasWebElement textAreaId(@Param("value") String value);

    @Name("Инпут")
    @FindBy(".//input")
    AtlasWebElement input();
}
