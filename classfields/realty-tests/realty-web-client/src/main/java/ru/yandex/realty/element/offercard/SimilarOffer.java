package ru.yandex.realty.element.offercard;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.ButtonWithTitle;

/**
 * @author kantemirov
 */
public interface SimilarOffer extends ButtonWithTitle, PhoneBlock {

    @Name("Кнопка добавить в избранное")
    @FindBy(".//button[contains(@class,'ItemAddToFavorite')]")
    AtlasWebElement addToFavButton();

    @Name("Кнопка добавить в сравнение")
    @FindBy(".//button[contains(@class,'OfferComparisonButton')]")
    AtlasWebElement addToComparisonButton();
}
