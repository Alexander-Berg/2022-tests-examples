package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

/**
 * @author kantemirov
 */
public interface ButtonWithTitle extends AtlasWebElement {

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//*[contains(@title,'{{ value }}')]")
    AtlasWebElement buttonWithTitle(@Param("value") String value);
}
