package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DeveloperChat extends AtlasWebElement {

    @Name("Крестик закрыть")
    @FindBy(".//span[contains(@class,'ChatApp__close')]")
    AtlasWebElement closeCross();

    @Name("Сообщения чата")
    @FindBy(".//div[contains(@class,'ChatMessage_received') or contains(@class, 'ChatMessage_sent')]")
    ElementsCollection<ChatMessage> messages();

    default ChatMessage firstMessage() {
        return messages().waitUntil(hasSize(greaterThan(0))).get(0);
    }

    @Name("Напишите сообщение...")
    @FindBy(".//textarea")
    AtlasWebElement textarea();

    @Name("Попап привязки телефона")
    @FindBy(".//div[contains(@class,'PhoneBindModal__content')]")
    AtlasWebElement phoneBindModal();
}
