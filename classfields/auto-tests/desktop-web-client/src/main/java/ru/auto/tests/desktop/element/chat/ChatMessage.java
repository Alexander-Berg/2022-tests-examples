package ru.auto.tests.desktop.element.chat;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 28.06.18
 */
public interface ChatMessage extends VertisElement {

    @Name("Текст сообщения")
    @FindBy(".//div[@class = 'ChatMessage__text']")
    VertisElement text();
}
