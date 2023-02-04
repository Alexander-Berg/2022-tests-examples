package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;

public interface MMMPopupModel extends VertisElement, WithCheckbox {

    @Name("Кнопка «>»")
    @FindBy(".//div[contains(@class, 'ListItem__arrow')]")
    VertisElement arrowButton();

    @Name("Название")
    @FindBy(".//div[contains(@class, 'ListItem__name')]")
    VertisElement name();
}