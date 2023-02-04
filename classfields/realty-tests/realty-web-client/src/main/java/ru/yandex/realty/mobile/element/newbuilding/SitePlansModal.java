package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface SitePlansModal extends AtlasWebElement {

    @Name("Ссылки на офферы")
    @FindBy(".//div[contains(@class, 'CardPlansOffersSerp__offer')]//div[contains(@class, 'Link__click-area')]")
    ElementsCollection<AtlasWebElement> snippetLinks();
}
