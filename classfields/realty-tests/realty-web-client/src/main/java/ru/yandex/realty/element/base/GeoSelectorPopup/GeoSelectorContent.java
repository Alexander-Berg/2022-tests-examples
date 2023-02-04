package ru.yandex.realty.element.base.GeoSelectorPopup;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;


public interface GeoSelectorContent extends AtlasWebElement {

    @Name("Кнопка «Область»")
    @FindBy(".//div[contains(@class,'MapButtonWithText') and contains(.,'Область')]")
    AtlasWebElement drawArea();

    @Name("Кнопка «Завершить»")
    @FindBy(".//div[contains(@class,'MapButtonWithText') and contains(.,'Завершить')]")
    AtlasWebElement finishDraw();
}
