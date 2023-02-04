package ru.yandex.webmaster.tests.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface Row extends AtlasWebElement {

    @Name("")
    @FindBy(".//td[contains(@class,'StarTable-Cell_name_url')]")
    AtlasWebElement urlName();
}
