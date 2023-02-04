package ru.auto.tests.desktop.mobile.element.chat;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.chat.SafeDealWidget;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 28.06.18
 */
public interface Chat extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//div[@class = 'ChatHead']")
    VertisElement header();

    @Name("Пресет «Безопасная сделка»")
    @FindBy(".//div[contains(@class, 'SafeDealPreset')]")
    VertisElement safeDealPreset();

    @Name("Виджет «Безопасная сделка»")
    @FindBy(".//div[@class = 'SafeDealWidget']")
    SafeDealWidget safeDealWidget();

}