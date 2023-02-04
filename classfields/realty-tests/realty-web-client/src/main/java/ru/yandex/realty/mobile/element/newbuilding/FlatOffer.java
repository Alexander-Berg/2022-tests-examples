package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface FlatOffer extends AtlasWebElement {

    @Name("Добавить в избранное")
    @FindBy(".//div[contains(@class,'CardPlansOffersSerp__favorites')]")
    AtlasWebElement addToFav();
}
