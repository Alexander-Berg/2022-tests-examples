package ru.auto.tests.desktop.element.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface HeaderAuthDropdown extends VertisElement, WithButton {

    @Name("Имя пользователя")
    @FindBy(".//div[contains(@class, 'HeaderUserMenu__name')]")
    VertisElement userName();

    @Name("Id пользователя")
    @FindBy(".//div[contains(@class, 'HeaderUserMenu__userId')]")
    VertisElement userId();
}