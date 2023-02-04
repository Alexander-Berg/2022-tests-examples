package ru.yandex.realty.mobile.element.listing;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.Link;

public interface SubscriptionForm extends AtlasWebElement, Button, Link {

    String SUBSCRIBE = "Подписаться";
    String CONFIRMATION = "Письмо с подтверждением отправлено\nна почту";
    String TERMS_OF_USE = "пользовательского соглашения";
    String PODPISKI = "Подписки";
    String REPEAT = "Повторить";

    @Name("Email")
    @FindBy(".//input")
    AtlasWebElement emailInput();

    @Name("Чекбокс «Подписаться на новости и рекламные рассылки»")
    @FindBy(".//label[contains(@class, 'Checkbox')]")
    AtlasWebElement checkbox();

    @Name("Описание")
    @FindBy(".//div[contains(@class, 'description')]")
    AtlasWebElement description();

    @Name("Подписка оформлена")
    @FindBy(".//div[contains(@class, 'Success')]")
    AtlasWebElement subscriptionSuccess();

    @Name("Произошла ошибка")
    @FindBy(".//div[contains(@class, 'Error')]")
    Button error();

    @Name("Очистка инпута")
    @FindBy(".//span[contains(@class, 'Input__clear')]")
    AtlasWebElement clearInput();

}
