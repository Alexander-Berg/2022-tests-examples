package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface VillageOffer extends Link {

    @Name("Добавить в избранное")
    @FindBy(".//button[contains(@class,'VillageOffersSerpItem__favorite')]")
    AtlasWebElement addToFav();
}
