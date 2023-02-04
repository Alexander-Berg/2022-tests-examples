package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface DiscountPopup extends Popup {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'VasDiscountPopup__title')]")
    VertisElement title();

    @Name("Кнопка покупки услуги")
    @FindBy(".//button[contains(@class, 'VasPromoItem__buyButton')]")
    VertisElement buyButton();

    @Name("Иконка закрытия поп-апа")
    @FindBy(".//div[contains(@class, 'Modal__closer')]/*")
    VertisElement closeIcon();
}
