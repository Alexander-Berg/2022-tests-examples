package ru.yandex.realty.mobile.element.subscriptions;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 21.08.17.
 */
public interface ResendConfirmToEmailPopup extends AtlasWebElement {

    @Name("Кнопка 'отмена'")
    @FindBy(".//span[contains(@class, 'link__inner')]")
    AtlasWebElement cancelButton();

    @Name("Кнопка 'Крестик'")
    @FindBy(".//i[contains(@class, 'popup__close')]")
    AtlasWebElement closePopupButton();
}
