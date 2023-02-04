package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface OfferPrice extends AtlasWebElement {

    @Name("Редактировать/Сохранить")
    @FindBy(".//button")
    AtlasWebElement button();

    @Name("Инпут")
    @FindBy(".//input")
    AtlasWebElement input();

    @Name("Цена за м²")
    @FindBy(".//div[contains(@class,'owner-offer-price__sub')]")
    AtlasWebElement byMeter();

    @Name("Валюта оффера")
    @FindBy(".//span[contains(@class,'MoneyAmountInput__currency')]")
    AtlasWebElement currency();
}
