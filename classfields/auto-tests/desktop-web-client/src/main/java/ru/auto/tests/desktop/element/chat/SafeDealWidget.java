package ru.auto.tests.desktop.element.chat;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface SafeDealWidget extends VertisElement, WithButton {

    String SEND_REQUEST = "Отправить запрос";
    String GO_TO_DEAL = "Перейти к сделке";
    String CANCEL_REQUEST = "Отменить запрос";
    String DETAILED = "Подробнее";


    @Name("Закрыть виджет")
    @FindBy(".//div[contains(@class, '_closer')]")
    VertisElement close();

}
