package ru.yandex.realty.element.archive;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface AddressSuggest extends AtlasWebElement {

    @Name("Список предлагаемых адресов")
    @FindBy(".//li")
    ElementsCollection<AtlasWebElement> addresses();
}
