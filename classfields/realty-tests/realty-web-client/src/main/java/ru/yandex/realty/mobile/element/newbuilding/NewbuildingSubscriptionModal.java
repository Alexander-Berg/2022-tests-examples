package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;

public interface NewbuildingSubscriptionModal extends AtlasWebElement, Button, Link {

    String SUBSCRIBE = "Подписаться";
    String CONFIRMATION = "Осталось только подтвердить адрес электронной почты";
    String TERMS_OF_USE = "пользовательского соглашения";
    String PODPISKI = "Подписки";
    String REPEAT = "Повторить";
    String TRY_ONE_MORE_TIME = "Попробуйте подписаться ещё раз";

    @Name("Закрыть")
    @FindBy(".//button[contains(@class, 'closeModalButton')]")
    AtlasWebElement close();

    @Name("Emai")
    @FindBy(".//input[contains(@class, 'TextInput')]")
    AtlasWebElement emailInput();

    @Name("Очистка инпута")
    @FindBy(".//span[contains(@class, 'Input__clear')]")
    AtlasWebElement clearInput();

    @Name("Чекбокс «Подписаться на новости и рекламные рассылки»")
    @FindBy(".//label[contains(@class, 'Checkbox')]")
    AtlasWebElement checkbox();

    @Name("Описание")
    @FindBy(".//div[contains(@class, 'description')]")
    AtlasWebElement description();

    @Name("Подписка оформлена")
    @FindBy(".//h2[contains(., 'Подписка оформлена')]")
    AtlasWebElement subscriptionSuccess();

}
