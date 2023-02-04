package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Header extends VertisElement, Link {

    String LOGIN = "Войти";

    @Name("Регион")
    @FindBy(".//button[contains(@class, 'LocationSuggest')]")
    VertisElement region();

    @Name("Лого «Объявления»")
    @FindBy(".//a[contains(@class, 'Link HeaderLogo')]")
    VertisElement oLogo();

    @Name("Чаты")
    @FindBy(".//button[contains(@class, 'Header__message')]")
    VertisElement chats();

    @Name("Бургер")
    @FindBy(".//button[contains(@class, 'Header__button')][2]")
    VertisElement burger();

    @Name("Кнопка «Назад»")
    @FindBy(".//button[contains(@class, '_backButton')]")
    VertisElement back();

}
