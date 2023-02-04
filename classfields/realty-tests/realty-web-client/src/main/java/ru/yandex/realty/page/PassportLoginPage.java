package ru.yandex.realty.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.passport.account.Account;

public interface PassportLoginPage extends BasePage {

    String NOT_NOW = "Не\u00a0сейчас";
    String LOGIN = "Войти";

    @Name("Инпут логина")
    @FindBy("//input[@id = 'passp-field-login']")
    AtlasWebElement login();

    @Name("Инпут пароля")
    @FindBy("//input[@id = 'passp-field-passwd']")
    AtlasWebElement password();

    @Name("Ссылка «Войти в другой аккаунт»")
    @FindBy("//a[contains(@class,'AddAccountButton')]")
    AtlasWebElement addAccount();

    @Step("Логинимся в интерфейсе паспорта")
    default void loginInPassport(Account account) {
        login().sendKeys(account.getLogin());
        button(LOGIN).click();
        password().sendKeys(account.getPassword());
        button(LOGIN).click();
        button(NOT_NOW).click();
    }

}
