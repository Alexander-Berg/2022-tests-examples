package ru.auto.tests.desktop.page.auth;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.auth.SocialBlock;
import ru.auto.tests.desktop.page.BasePage;

public interface AuthPage extends BasePage {

    String SMS_CODE = "Код из смс";
    String MAIL_CODE = "Код из письма";
    String PHONE_EMAIL_INPUT = "Телефон или электронная почта";
    String CONTINUE_BUTTON = "Продолжить";

    @Name("Заголовок")
    @FindBy("//div[contains(@class, 'AuthForm__title')]")
    VertisElement title();

    @Name("Основное окно")
    @FindBy("//div[contains(@class, 'AuthForm')]")
    VertisElement authForm();

    @Name("Поле ввода «Телефон и электронная почта»")
    @FindBy("//input[@class = 'TextInput__control']")
    VertisElement phoneInput();

    @Name("Сообщение об ошибке")
    @FindBy("//span[@class = 'TextInput__error']")
    VertisElement notifyError();

    @Name("Уведомление «Вам отправлено смс с кодом подтверждения»")
    @FindBy("//div[@class = 'AuthFormCodeInput__text']")
    VertisElement notifyCode();

    @Name("Ссылка на повторную отправку кода")
    @FindBy("//span[@class = 'AuthForm__link']")
    VertisElement resend();

    @Name("Ссылка на таймер - ожидания для повторой отправки кода")
    @FindBy("//div[@class = 'AuthFormCodeInput__send-again-in-time']")
    VertisElement codeTimer();

    @Name("Ссылка на авторизацию через соцсети")
    @FindBy("//div[@class = 'AuthFormSocialLogin__providers']")
    SocialBlock social();

    @Name("Ссылка на авторизацию через Яндекс.ID")
    @FindBy("//div[contains(@class, 'YandexIdLoginButton')]//a")
    VertisElement yandexIdLoginButton();

    @Name("Крестик для удаления введенного значения")
    @FindBy("//i[@class = 'TextInput__clear TextInput__clear_visible']")
    SocialBlock deleteButton();

    @Name("Поп-ап авторизации через Яндекс")
    @FindBy("//html")
    YandexAuthPopup yandexAuthPopup();

    @Name("Поп-ап авторизации через Mail.ru")
    @FindBy("//html")
    MailruAuthPopup mailruAuthPopup();

    @Name("Логотип-ссылка на главную страницу")
    @FindBy("//div[@class = 'AuthForm__logo-image']")
    SocialBlock logoButton();
}
