package ru.auto.tests.desktop.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ListingSubscription extends VertisElement {

    @Name("Поле ввода email")
    @FindBy(".//input[@name = 'email']")
    VertisElement input();

    @Name("Сообщение об ошибке")
    @FindBy(".//span[contains(@class, 'TextInput__error')]")
    VertisElement errorText();

    @Name("Кнопка подписки")
    @FindBy(".//button")
    VertisElement subscribeButton();
}