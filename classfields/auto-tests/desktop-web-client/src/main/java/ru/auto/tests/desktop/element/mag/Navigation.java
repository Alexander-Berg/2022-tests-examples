package ru.auto.tests.desktop.element.mag;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Navigation extends VertisElement, WithButton {

    @Name("Кнопка закрытия")
    @FindBy(".//button[contains(@class, 'ListingTagNavigation__closeButton')]")
    VertisElement closeButton();
}