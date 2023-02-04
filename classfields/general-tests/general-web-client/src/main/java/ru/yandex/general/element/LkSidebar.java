package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface LkSidebar extends VertisElement, Link, Image {

    @Name("Имя юзера")
    @FindBy(".//span[contains(@class, 'userName')]")
    VertisElement userName();

    @Name("Email юзера")
    @FindBy(".//span[contains(@class, 'userEmail')]")
    VertisElement userEmail();

    @Name("Аватар")
    @FindBy(".//div[contains(@class, 'UserInfo__avatar')]")
    VertisElement avatar();

}
