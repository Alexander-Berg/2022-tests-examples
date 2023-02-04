package ru.yandex.realty.element.promocode;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface PromocodeFeature extends AtlasWebElement {

    @Name("Значение промо бонуса")
    @FindBy(".//div[@class='promocode-item__amount']")
    AtlasWebElement amount();

    @Name("Длительность бонуса")
    @FindBy(".//div[@class='promocode-item__deadline']")
    ElementsCollection<AtlasWebElement> deadline();
}
