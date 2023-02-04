package ru.yandex.realty.element.map;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.saleads.InputField;

public interface AddressPopup extends InputField {

    @Name("Саджест ")
    @FindBy(".//ul[contains(@class, 'Suggest__list')]//li")
    ElementsCollection<AtlasWebElement> suggest();
}
