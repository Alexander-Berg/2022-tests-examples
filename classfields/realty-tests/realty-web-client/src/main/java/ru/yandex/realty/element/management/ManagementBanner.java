package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 28.07.17.
 */
public interface ManagementBanner extends AtlasWebElement {

    @Name("Кнопка «Оплатить»")
    @FindBy(".//button[contains(@class, 'promo-button')]")
    AtlasWebElement promoButton();
}
