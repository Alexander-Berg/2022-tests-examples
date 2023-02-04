package ru.yandex.realty.element.popups;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 01.08.17.
 */
public interface OfferPreview extends AtlasWebElement {

    @Name("Картинка оффера")
    @FindBy(".//div[@class='preview__content']")
    AtlasWebElement image();
}
