package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface IconTypeCross extends AtlasWebElement {

    @Name("Крестик закрытия")
    @FindBy(".//i[@class='Icon Icon_type_cross']")
    AtlasWebElement iconTypeCross();
}
