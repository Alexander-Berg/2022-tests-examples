package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface UserInfo extends VertisElement {

    @Name("Имя юзера")
    @FindBy(".//span[contains(@class, 'userName')]")
    VertisElement userName();

    @Name("Email юзера")
    @FindBy(".//span[contains(@class, 'email')]")
    VertisElement userEmail();

    @Name("Id юзера")
    @FindBy(".//span[contains(@class, 'PersonalContactsMain__userId')]")
    VertisElement userId();

    @Name("Аватар")
    @FindBy(".//div[contains(@class, 'PersonalContactsMain__avatar_')]")
    VertisElement avatar();

}
