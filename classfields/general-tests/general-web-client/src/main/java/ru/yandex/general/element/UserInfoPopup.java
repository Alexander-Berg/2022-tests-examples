package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface UserInfoPopup extends VertisElement, Link, Image {

    @Name("Ссылка на паспорт юзера")
    @FindBy("//a[contains(@class, 'UserInfo__popupLink')]")
    Link passportLink();

}
