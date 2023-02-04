package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.ButtonWithTitle;

public interface SimilarTouchOffer extends ButtonWithTitle, Link {

    @Name("Ссылка похожего оффера")
    @FindBy(".//a[contains(@class,'OfferSerpSnippet__itemLink')]")
    AtlasWebElement offerLink();

    @Name("Добавить в избранное")
    @FindBy("//*[contains(@class,'SerpItem__favor')]")
    AtlasWebElement addToFav();
}