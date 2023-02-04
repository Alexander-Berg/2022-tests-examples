package ru.yandex.realty.mobile.element.subscriptions;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 21.08.17.
 */
public interface ConfirmEmailPopup extends AtlasWebElement {

    @Name("Кнопка подтверждения")
    @FindBy(".//div[contains(@class, 'confirm__resolver')]//button")
    AtlasWebElement confirmEmailButton();

    @Name("Кнопка отмены")
    @FindBy(".//i[contains(@class, 'popup__close')]")
    AtlasWebElement cancelButton();
}
