package ru.yandex.realty.element.comparison;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 29.06.17.
 */
public interface Contact extends AtlasWebElement {

    @Name("Раскрытое окно контактов")
    @FindBy(".//div[contains(@class, 'ItemShowPhone_phones')]")
    AtlasWebElement shownContacts();

    @Name("Кнопка «Показать контакты»")
    @FindBy(".//button")
    AtlasWebElement showContactsButton();
}