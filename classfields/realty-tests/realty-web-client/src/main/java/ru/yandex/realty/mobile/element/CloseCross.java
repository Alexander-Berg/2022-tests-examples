package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface CloseCross extends AtlasWebElement {

    @Name("Крестик закрыть")
    @FindBy(".//i[contains(@class,'Icon_type_cross')]")
    AtlasWebElement closeCross();

    @Name("Крестик закрыть")
    @FindBy(".//*[contains(@class,'CrossIconHeader__icon')]")
    AtlasWebElement closeCrossHeader();
}
