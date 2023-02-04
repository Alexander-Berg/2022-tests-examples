package ru.yandex.arenda.element.common;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Button extends AtlasWebElement {

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//button[contains(.,'{{ value }}')]")
    ArendaElement button(@Param("value") String value);

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//button[.='{{ value }}']")
    ArendaElement exactButton(@Param("value") String value);

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//a[contains(.,'{{ value }}')]")
    ArendaElement buttonRequest(@Param("value") String value);

    @Name("Кнопка")
    @FindBy(".//button")
    ArendaElement button();
}
