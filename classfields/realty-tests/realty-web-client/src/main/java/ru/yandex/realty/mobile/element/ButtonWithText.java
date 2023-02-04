package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface ButtonWithText extends AtlasWebElement {

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//button[.='{{ value }}']")
    AtlasWebElement buttonWithText(@Param("value") String value);
}
