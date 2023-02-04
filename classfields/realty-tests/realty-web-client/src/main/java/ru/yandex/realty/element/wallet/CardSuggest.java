package ru.yandex.realty.element.wallet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface CardSuggest extends AtlasWebElement {

    @Name("Привязанные карты")
    @FindBy(".//div[contains(@class,'menu__item')]")
    ElementsCollection<AtlasWebElement> cardList();
}
