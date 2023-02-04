package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.TradeInPopup;

public interface WithTradeIn {

    @Name("Кнопка «Обмен на мой авто»")
    @FindBy("//button[contains(@class, 'TradeinButton__button')]")
    VertisElement tradeInButton();

    @Name("Поп-ап трейд-ина")
    @FindBy("//div[contains(@class, 'Tradein-module__modal')]//div[contains(@class, 'Modal__content')] | " +
            "//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'TradeinViewButton__modal')]")
    TradeInPopup tradeInPopup();
}