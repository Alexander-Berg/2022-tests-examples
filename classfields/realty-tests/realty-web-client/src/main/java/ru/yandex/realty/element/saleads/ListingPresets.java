package ru.yandex.realty.element.saleads;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface ListingPresets extends AtlasWebElement {

    @Name("Пресет «{{ value }}»")
    @FindBy(".//a[contains(@class, 'OffersSerpSuggestionPresets__button') and contains(.,'{{ value }}')]")
    AtlasWebElement preset(@Param("value") String value);

    @Name("Кнопка «Ещё»")
    @FindBy(".//span[contains(@class, 'OffersSerpSuggestionPresets__button') and contains(.,'Ещё')]")
    AtlasWebElement showMoreButton();

    @Name("Кнопка «Ещё»")
    @FindBy(".//span[contains(@class, 'OffersSerpSuggestionPresets__button') and contains(.,'Свернуть')]")
    AtlasWebElement collapseButton();
}
