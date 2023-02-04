package ru.yandex.realty.mobile.element.specproject;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface SpecProjMenu extends AtlasWebElement {

    @Name("Секция {{ value }}")
    @FindBy(".//span[contains(@class,'SamoletMenu__link') and contains(.,'{{ value }}')]")
    AtlasWebElement section(@Param("value") String value);
}
