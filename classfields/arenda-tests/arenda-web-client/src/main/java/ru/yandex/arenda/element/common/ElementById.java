package ru.yandex.arenda.element.common;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface ElementById extends AtlasWebElement {

    @Name("Элемент с id=«{{ value }}»")
    @FindBy(".//*[@id='{{ value }}']")
    AtlasWebElement byId(@Param("value") String value);
}
