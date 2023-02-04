package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ChatPage extends BasePage {

    @Name("Контент")
    @FindBy("//div[contains(@class, 'ChatAuthPage')]")
    VertisElement content();

    @Name("Кнопка «Войти»")
    @FindBy("//div[contains(@class, 'PopupEmptyPlaceholder__buttons')]//a[contains(@class, 'Button')]")
    VertisElement loginButton();
}