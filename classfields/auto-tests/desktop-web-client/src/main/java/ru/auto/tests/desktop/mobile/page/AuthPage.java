package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.auth.SocialBlock;

public interface AuthPage extends BasePage {

    String CONTINUE_BUTTON = "Продолжить";
    String MAIL_CODE = "Код из письма";
    String SMS_CODE = "Код из смс";

    @Name("Поле ввода «Телефон и электронная почта»")
    @FindBy("//input[@class = 'TextInput__control']")
    VertisElement phoneInput();

    @Name("Ссылка на авторизацию через соцсети")
    @FindBy("//div[@class = 'AuthFormSocialLogin__providers']")
    SocialBlock social();

    @Name("Войти через электронную почту")
    @FindBy("//a[@class = 'AuthForm__link']")
    VertisElement mailLink();

    @Name("Основное окно")
    @FindBy("//div[contains(@class, 'AuthForm')]")
    VertisElement authForm();

    @Step("Авторизуемся с телефоном")
    default void phoneAuthorize(String phone) {
        input("Номер телефона", phone);
        input(SMS_CODE, "1234");
    }
}
