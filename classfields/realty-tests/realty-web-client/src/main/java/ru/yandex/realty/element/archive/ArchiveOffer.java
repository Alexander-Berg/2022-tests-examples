package ru.yandex.realty.element.archive;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface ArchiveOffer extends AtlasWebElement {

    @Name("Ссылка на карточку оффера")
    @FindBy(".//a")
    AtlasWebElement offerLink();

    @Name("Блок «Метраж, комнатность»")
    @FindBy(".//div[contains(@class,'OffersArchiveSearchOffers__cell')]" +
            "/*[contains(@class,'OffersArchiveSearchOffers__title')]")
    AtlasWebElement mainOfferInfo();

    @Name("Блок цен")
    @FindBy(".//div[contains(@class,'OffersArchiveSearchOffers__price')]")
    AtlasWebElement priceOfferInfo();
}
