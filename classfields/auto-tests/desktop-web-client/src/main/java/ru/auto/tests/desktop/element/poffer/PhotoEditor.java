package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface PhotoEditor extends VertisElement, WithButton {

    @Name("img")
    @FindBy(".//img")
    VertisElement image();

    @Name("Кнопка «Повернуть»")
    @FindBy(".//div[contains(@class, 'photo-item__rotate')]")
    VertisElement rotateButton();

    @Name("Кнопка «Удалить»")
    @FindBy(".//div[contains(@class, 'photo-item__rem')]")
    VertisElement deleteButton();
}