package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Screen extends VertisElement, Input, Link, Button, Switcher {

    @Name("Инпут «Найти адрес»")
    @FindBy("//input[@placeholder = 'Найти адрес']")
    VertisElement findAddressInput();

    @Name("Кнопка закрытия экрана")
    @FindBy(".//button[contains(@class, '_closeButton')]")
    VertisElement close();

    @Name("Кнопка назад")
    @FindBy(".//button[contains(@class, '_back')]")
    VertisElement back();

}
