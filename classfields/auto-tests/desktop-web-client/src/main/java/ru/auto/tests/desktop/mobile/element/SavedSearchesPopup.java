package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 05.03.18
 */
public interface SavedSearchesPopup extends VertisElement, WithInput {

    @Name("Кнопка «Отправить»")
    @FindBy(".//div[contains(@class, 'SubscriptionModalEmail__control')]//button")
    VertisElement sendButton();

    @Name("Ссылка «Посмотреть сохранённые поиски»")
    @FindBy(".//a[contains(@class, 'searches-link')]")
    VertisElement showSearchesUrl();

    @Name("Кнопка закрытия поп-апа")
    @FindBy(".//div[contains(@class, 'Modal__closer')]")
    VertisElement closeButton();
}