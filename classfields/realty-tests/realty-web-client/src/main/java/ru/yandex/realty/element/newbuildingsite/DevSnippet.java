package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.ButtonWithTitle;

public interface DevSnippet extends ButtonWithTitle {

    @Name("Кнопка показа телефона")
    @FindBy(".//button[contains(@class, 'SnippetContacts__button')]")
    AtlasWebElement showPhoneButton();
}
