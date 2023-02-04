package ru.yandex.realty.element.archive;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface OffersArchiveAddressForm extends AtlasWebElement {

    @Name("Строка поиска")
    @FindBy(".//input")
    AtlasWebElement input();

    @Name("Кнопка «Найти»")
    @FindBy(".//button")
    AtlasWebElement submitButton();

    @Name("Крестик очистки")
    @FindBy(".//span[contains(@class, 'TextInput__clear_visible')]")
    AtlasWebElement clearButton();

    @Name("Саджест выбора адреса")
    @FindBy(".//ul[contains(@class, 'OffersArchiveAddressForm__suggest-list')]")
    AddressSuggest addressSuggest();
}
