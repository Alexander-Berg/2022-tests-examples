package ru.yandex.arenda.element.common;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Select extends AtlasWebElement {

    @Name("Селектор «{{ value }}»")
    @FindBy(".//select[@id='{{ value }}']")
    AtlasWebElement selector(@Param("value") String value);

    @Name("Опция «{{ value }}»")
    @FindBy(".//option[.='{{ value }}']")
    AtlasWebElement option(@Param("value") String value);
}
