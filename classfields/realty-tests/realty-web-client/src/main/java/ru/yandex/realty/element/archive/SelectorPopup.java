package ru.yandex.realty.element.archive;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface SelectorPopup extends AtlasWebElement {

    @Name("«{{ value }}»")
    @FindBy(".//div[contains(@class, 'Menu__item') and contains(.,'{{ value }}')]")
    AtlasWebElement option(@Param("value") String value);
}
