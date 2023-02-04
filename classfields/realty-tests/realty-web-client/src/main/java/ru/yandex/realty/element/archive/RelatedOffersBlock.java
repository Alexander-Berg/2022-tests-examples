package ru.yandex.realty.element.archive;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface RelatedOffersBlock extends AtlasWebElement {

    @Name("Список похожих объявлений")
    @FindBy(".//a")
    ElementsCollection<AtlasWebElement> relatedOffers();
}
