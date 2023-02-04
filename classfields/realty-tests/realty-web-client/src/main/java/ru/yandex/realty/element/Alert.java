package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Alert extends AtlasWebElement {

    @Name("Попап «{{ value }}»")
    @FindBy("//div[@role='alert' and contains(.,'{{ value }}')]")
    AtlasWebElement alert(@Param("value") String value);
}
