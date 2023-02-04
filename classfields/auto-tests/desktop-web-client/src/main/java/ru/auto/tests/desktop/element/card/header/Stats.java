package ru.auto.tests.desktop.element.card.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Stats extends VertisElement {

    @Name("Количество звонков")
    @FindBy(".//div[contains(@class, 'CardHead__calls')] | " +
            ".//li[contains(@class, 'card__stat-item_icon_calls')]")
    VertisElement callHistoryButton();

    @Name("Счетчик «Добавлено в избранное» (сердечко)")
    @FindBy(".//div[contains(@class, 'CardHead__favorites')]")
    VertisElement favoriteCounter();
}