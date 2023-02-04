package ru.yandex.realty.element.wallet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 14.07.17.
 */
public interface RoboKassaBlock extends AtlasWebElement {

    @Name("Поле ввода номера карты")
    @FindBy(".//input[contains(@name, 'CardNumber')]")
    AtlasWebElement cardNumberField();

    @Name("Поле ввода cvc")
    @FindBy(".//input[contains(@name, 'CVC')]")
    AtlasWebElement CVCField();

    @Name("Поле ввода email")
    @FindBy(".//input[contains(@name, 'EMail')]")
    AtlasWebElement eMailField();

    @Name("Кнопка «Перейти к оплате»")
    @FindBy(".//button[contains(., 'Оплатить')]")
    AtlasWebElement submitButton();

    @Name("Кнопка для отправки ответа об успешной оплате")
    @FindBy("//div[@class='btnGood' and contains(.,'Успешная оплата')]")
    AtlasWebElement successfulPaymentButton();
}

