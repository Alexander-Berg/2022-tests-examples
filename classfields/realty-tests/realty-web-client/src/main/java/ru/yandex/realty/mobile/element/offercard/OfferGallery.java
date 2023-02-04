package ru.yandex.realty.mobile.element.offercard;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.mobile.element.CloseCross;

public interface OfferGallery extends Link, CloseCross {

    @Name("Кнопка добавить в избранное")
    @FindBy(".//button[contains(@class,'ItemAddToFavorite')]")
    AtlasWebElement addToFav();
}
