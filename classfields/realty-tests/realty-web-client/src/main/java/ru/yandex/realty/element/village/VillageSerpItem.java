package ru.yandex.realty.element.village;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface VillageSerpItem extends Button {

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//button[contains(@class,'SnippetContacts__button') and not(contains(@class,'SnippetContacts__callbackButton'))]")
    AtlasWebElement showPhoneButton();
}
