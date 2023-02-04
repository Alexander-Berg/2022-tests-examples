package ru.auto.tests.desktop.element.cabinet.users;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface UserBlock extends VertisElement, WithButton {

    @Name("Имя пользователя")
    @FindBy(".//div[contains(@class, 'UsersRolesUserItem__name')]")
    VertisElement userName();
}