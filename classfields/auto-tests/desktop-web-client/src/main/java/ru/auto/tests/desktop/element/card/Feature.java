package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithActivePopup;
import ru.auto.tests.desktop.component.WithButton;

public interface Feature extends VertisElement, WithActivePopup, WithButton {

    @Name("Тултип")
    @FindBy(".//div[contains(@class, '__tooltip')]")
    VertisElement tooltip();
}