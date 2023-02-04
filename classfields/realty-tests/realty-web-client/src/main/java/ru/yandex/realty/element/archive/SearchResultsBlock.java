package ru.yandex.realty.element.archive;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface SearchResultsBlock extends AtlasWebElement {

    @Name("Список объявлений")
    @FindBy(".//div[contains(@class, 'OffersArchiveSearchOffers__row')]")
    ElementsCollection<ArchiveOffer> archiveOffers();
}
