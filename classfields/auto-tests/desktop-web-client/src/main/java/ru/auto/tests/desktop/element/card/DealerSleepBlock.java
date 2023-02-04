package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface DealerSleepBlock extends VertisElement, WithInput, WithButton {

    @Name("Кнопка «Перезвоните мне»")
    @FindBy(".//button")
    VertisElement submitButton();
}