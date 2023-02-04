package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by vicdev on 24.04.17.
 */
public interface RegisterPopup extends AtlasWebElement {

    @Name("Кнопка 'Сохранить'")
    @FindBy("//div[contains(@class, 'contacts__footer')]//button")
    AtlasWebElement saveButton();

}
