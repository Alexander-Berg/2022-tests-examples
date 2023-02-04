package ru.auto.tests.desktop.page.auth;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.page.BasePage;

public interface MailruAuthPopup extends BasePage {

    @Name("Поле ввода логина")
    @FindBy(".//input[@name = 'Login']")
    VertisElement loginInput();

    @Name("Поле ввода пароля")
    @FindBy(".//input[@name = 'Password']")
    VertisElement passwordInput();

    @Name("Кнопка «Войти»")
    @FindBy(".//button[@type = 'submit']")
    VertisElement submitButton();
}