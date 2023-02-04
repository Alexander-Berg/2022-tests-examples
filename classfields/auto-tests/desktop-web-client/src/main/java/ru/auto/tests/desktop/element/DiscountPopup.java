package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface DiscountPopup extends Popup {

    @Name("Кнопка «Включить»")
    @FindBy(".//div[contains(@class, 'PromoPopupDiscount__getService')]")
    VertisElement turnOnButton();
}