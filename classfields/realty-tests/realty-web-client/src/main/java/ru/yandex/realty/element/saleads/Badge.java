package ru.yandex.realty.element.saleads;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface Badge extends AtlasWebElement {

    @Name("Крестик удаления бэйджа")
    @FindBy(".//i")
    AtlasWebElement clearGeo();

}
