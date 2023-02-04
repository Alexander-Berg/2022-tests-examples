package ru.auto.tests.desktop.element.chat;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 28.06.18
 */
public interface ChatItem extends VertisElement {

    @Name("Имя пользователя")
    @FindBy(".//span[@class = 'ChatListItem__username']")
    VertisElement userName();

    @Name("Последнее сообщение")
    @FindBy(".//div[@class = 'ChatListItem__last-message']")
    VertisElement lastMessage();

    @Name("Значок отключенных уведомлений")
    @FindBy(".//span[@class = 'ChatListItem__muted']")
    VertisElement muted();

    @Name("Пользователь заблокирован")
    @FindBy(".//div[@class = 'ChatListItem__blocked']")
    VertisElement blocked();
}
