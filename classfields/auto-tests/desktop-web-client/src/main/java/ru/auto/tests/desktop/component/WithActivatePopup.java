package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.lk.ActivatePopup;

public interface WithActivatePopup {

    @Name("Поп-ап платной активации объявления")
    @FindBy("//div[contains(@class, 'OfferActivateDialog')]//div[contains(@class, 'Modal__content')]")
    ActivatePopup activatePopup();
}