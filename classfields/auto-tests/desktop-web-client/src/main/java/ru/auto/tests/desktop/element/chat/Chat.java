package ru.auto.tests.desktop.element.chat;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 28.06.18
 */
public interface Chat extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//div[@class = 'ChatHead']")
    VertisElement header();

    @Name("Закрыть чат")
    @FindBy(".//span[@class = 'ChatApp__close']")
    VertisElement close();

    @Name("Разрешить уведомления")
    @FindBy(".//div[@class = 'RequestNotifications']")
    VertisElement permitNotification();

    @Name("Список чатов")
    @FindBy(".//div[@class = 'ChatList']//div[contains(@class, 'ChatListItem ') or contains(@class, 'ChatListItem_selected')]")
    ElementsCollection<ChatItem> chatItems();

    @Name("Многоточие")
    @FindBy(".//div[@class = 'ChatHead__more-button']")
    VertisElement ellipsis();

    @Name("Поп-ап «Меню чата»")
    @FindBy(".//div[@class = 'ChatMenu']")
    ChatMenu chatMenu();

    @Name("Сообщения")
    @FindBy(".//div[contains(@class, 'ChatMessage ')]")
    ElementsCollection<ChatMessage> chatMessages();

    @Name("Прикрепить файл")
    @FindBy(".//label[@class = 'ChatInput__file-label']")
    VertisElement addFile();

    @Name("Строка ввода нового сообщения")
    @FindBy(".//div[@class = 'ChatInput']//textarea")
    VertisElement newMessage();

    @Name("Отправить сообщение")
    @FindBy(".//span[@class = 'ChatInput__send-button']")
    VertisElement sendMessage();

    @Name("Кнопка «Разблокировать»")
    @FindBy(".//div[contains(@class, 'ChatApp__blocked')]")
    VertisElement unlocked();

    @Name("Пресет «Безопасная сделка»")
    @FindBy(".//div[contains(@class, 'SafeDealPreset')]")
    VertisElement safeDealPreset();

    @Name("Виджет «Безопасная сделка»")
    @FindBy(".//div[@class = 'SafeDealWidget']")
    SafeDealWidget safeDealWidget();

    default ChatItem chatItem(int i) {
        return chatItems().should(hasSize(greaterThan(i))).get(i);
    }

    default ChatItem chatItem(String name) {
        return chatItems().filter(r -> r.userName().getText().equals(name)).should(not(empty())).get(0);
    }

    default ChatItem chatItemById(String id) {
        return chatItems().filter(r -> r.getAttribute("data-chat-id").equals(id)).should(not(empty())).get(0);
    }

    default ChatMessage chatMessage(int i) {
        return chatMessages().should(hasSize(greaterThan(i))).get(i);
    }
}
