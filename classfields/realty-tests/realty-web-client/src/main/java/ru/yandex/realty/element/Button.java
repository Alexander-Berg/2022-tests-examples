package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Button extends AtlasWebElement {

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//button[contains(.,'{{ value }}')]")
    RealtyElement button(@Param("value") String value);

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//button[contains(.,'{{ value }}')]")
    RealtyElement buttonWithClickIf(@Param("value") String value);

    @Name("Кнопка")
    @FindBy(".//button")
    RealtyElement button();

}
