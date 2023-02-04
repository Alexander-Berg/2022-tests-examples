package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Tab extends AtlasWebElement {

    @Name("Таб «{{ value }}»")
    @FindBy(".//li[contains(@class,'Tab__container')]/a[contains(.,'{{ value }}')]")
    AtlasWebElement tab(@Param("value") String value);
}
