package ru.auto.tests.desktop.element.chat;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 28.06.18
 */
public interface ChatMenu extends VertisElement {

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//div[contains(@class, 'ChatMenu__item')][contains(., '{{ name }}')]")
    VertisElement chatItem(@Param("name") String name);
}
