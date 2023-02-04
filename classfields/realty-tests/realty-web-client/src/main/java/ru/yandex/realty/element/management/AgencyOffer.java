package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;

public interface AgencyOffer extends Button, Link {

    @Name("Сообщение оффера")
    @FindBy(".//div[contains(@class, 'MessagePanel__wrap')]")
    AtlasWebElement offerMessage();

    @Name("Кнопка добавления фотографии")
    @FindBy(".//button[@class='agency-offer-photo__button']")
    AtlasWebElement addFotoButton();

    @Name("Сообщение оффера")
    @FindBy(".//div[contains(@class,'agency-offer-messages')]")
    AtlasWebElement message();

    @Name("Чекбокс выделения оффера")
    @FindBy(".//label")
    AtlasWebElement checkbox();

    @Name("Инфо оффера")
    @FindBy(".//div[contains(@class,'agency-offer-object')]")
    AgencyOfferInfo agencyOfferLink();

    @Name("Блок цены")
    @FindBy(".//div[contains(@class,'agency-offer-price')]")
    PriceOfAgentOffer price();

    @Name("Галерея фотографий оффера")
    @FindBy(".//div[@class='gallery']")
    AtlasWebElement offerGallery();

    @Name("Кнопка Пакет турбо")
    @FindBy(".//button[contains(@class,'VasServiceBuyButton_type_turboSale')]")
    AtlasWebElement buyTurbo();

    @Name("Кнопка Поднятие")
    @FindBy(".//button[contains(@class,'VasServiceBuyButton_type_raising')]")
    AtlasWebElement buyRaising();

    @Name("Кнопка Продвижение")
    @FindBy(".//button[contains(@class,'VasServiceBuyButton_type_promotion')]")
    AtlasWebElement buyPromotion();

    @Name("Кнопка Премиум")
    @FindBy(".//button[contains(@class,'VasServiceBuyButton_type_premium')]")
    AtlasWebElement buyPremium();
}
