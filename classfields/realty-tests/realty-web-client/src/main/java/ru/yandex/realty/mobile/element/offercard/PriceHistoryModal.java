package ru.yandex.realty.mobile.element.offercard;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.mobile.element.CloseCross;

public interface PriceHistoryModal extends Button, InputField, CloseCross {

    String SUBSCRIBE = "Подписаться";
    String EMAIL = "Электронная почта";
    String BUTTON_DISABLED = "Button_disabled";

    @Name("Кнопка «История изменения цены»")
    @FindBy(".//li[@class='OfferPriceHistory__item']")
    ElementsCollection<AtlasWebElement> priceHistoryItems();

    @Name("Сообщение о подписке")
    @FindBy(".//div[contains(@class,'OfferCardPriceHistory__subscription')]")
    Link subscriptionMessage();
}
