package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 24.07.17.
 */
public interface PriceElement extends AtlasWebElement {

    @Name("Цена")
    @FindBy(".//span[contains(@class, 'manage-table__price-cell-wrapper')]")
    AtlasWebElement sum();

    @Name("Валюта")
    @FindBy(".//span[contains(@class, 'i-font_face')]")
    AtlasWebElement currency();
}
