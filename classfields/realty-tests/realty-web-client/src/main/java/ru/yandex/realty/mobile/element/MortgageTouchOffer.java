package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface MortgageTouchOffer extends Link {

    @Name("Добавить в избранное")
    @FindBy(".//div[contains(@class,'OfferSliderSnippet__favorites')]")
    AtlasWebElement addToFav();

}
