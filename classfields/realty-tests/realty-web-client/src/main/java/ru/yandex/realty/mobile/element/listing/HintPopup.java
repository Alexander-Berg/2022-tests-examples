package ru.yandex.realty.mobile.element.listing;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface HintPopup extends AtlasWebElement {

    @Name("Название продавца")
    @FindBy(".//div[contains(@class, 'sellerName')]")
    AtlasWebElement sellerName();

    @Name("Лого")
    @FindBy(".//img")
    AtlasWebElement logo();

    @Name("Реклама")
    @FindBy(".//div[contains(@class, 'sellerAds')][contains(., 'реклама')]")
    AtlasWebElement adsText();

}
