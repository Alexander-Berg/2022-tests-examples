package ru.yandex.realty.element.samolet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface SamoletGeoSelectorPopup extends AtlasWebElement {

    @Name("Элемент списка «{{ value }}»")
    @FindBy(".//span[contains(@class, 'SamoletMenu__geoSelectorPopupItem') and contains(.,'{{ value }}')]")
    AtlasWebElement geoSelectorItem(@Param("value") String value);

    @Name("Крестик закрытия")
    @FindBy(".//*[contains(@class,'SamoletMenu__geoSelectorPopupClose')]")
    AtlasWebElement closeCross();
}
