package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CallHistoryPromo extends VertisElement {

    @Name("Кнопка «Хорошо, спасибо»")
    @FindBy("//span[contains(@class, 'CardCallsHistoryPromo__link')]")
    VertisElement okButton();
}
