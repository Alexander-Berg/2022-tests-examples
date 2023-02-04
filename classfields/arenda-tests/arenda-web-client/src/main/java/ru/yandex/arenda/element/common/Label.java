package ru.yandex.arenda.element.common;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Label extends AtlasWebElement {

    @Name("Чекбокс «{{ value }}»")
    @FindBy(".//div[./label[.='{{ value }}']]")
    AtlasWebElement divWithLabel(@Param("value") String value);

    @Name("Чекбокс «{{ value }}»")
    @FindBy(".//label[.='{{ value }}']")
    AtlasWebElement label(@Param("value") String value);

    @Name("Чекбокс «{{ value }}»")
    @FindBy(".//label[contains(.,'{{ value }}')]")
    AtlasWebElement labelThatContains(@Param("value") String value);
}
