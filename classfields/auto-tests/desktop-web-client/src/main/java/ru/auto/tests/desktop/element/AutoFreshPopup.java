package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithSelect;

public interface AutoFreshPopup extends VertisElement, WithSelect {

    @Name("Кнопка «Включить автоподнятие»")
    @FindBy("//button[contains(@class, 'AutorenewModal__onAction')]")
    VertisElement turnOnButton();

    @Name("Кнопка «Выключить автоподнятие»")
    @FindBy("//span[contains(@class, 'AutorenewModal__action')]")
    VertisElement turnOffButton();
}