package ru.yandex.realty.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by vicdev on 24.04.17.
 */
public interface DomikPopup extends AtlasWebElement {

    @Name("Попап логина на Пасспорте")
    @FindBy("//div[contains(@class, 'passp-auth')]")
    AtlasWebElement domikPopup();

}
