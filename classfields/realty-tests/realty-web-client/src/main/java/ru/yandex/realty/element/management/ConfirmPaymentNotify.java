package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface ConfirmPaymentNotify extends AtlasWebElement {

    @Name("Нотификация о прохождении оплаты")
    @FindBy(".//div[contains(@class, 'confirm__controls')]//button")
    AtlasWebElement closeButton();
}
