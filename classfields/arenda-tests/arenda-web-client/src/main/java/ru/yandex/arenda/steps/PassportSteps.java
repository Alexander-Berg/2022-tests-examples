package ru.yandex.arenda.steps;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.experimental.Accessors;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.config.ArendaWebConfig;

@Accessors(chain = true)
public class PassportSteps extends WebDriverSteps {

    @Inject
    private ArendaWebConfig config;

    @Inject
    private UrlSteps urlSteps;

    @Step("Авторизация для аккаунта: {account}")
    public void login(Account account) {
        urlSteps.login().queryParam("host", config.getPassportTestURL().toString() + "passport")
                .queryParam("mode", "auth")
                .queryParam("login", account.getLogin())
                .queryParam("passwd", account.getPassword()).open();
    }

    @Step("Авторицация для логин: {login}, пароль: {password}")
    public void login(String login, String password) {
        urlSteps.login().queryParam("host", config.getPassportTestURL().toString() + "passport")
                .queryParam("mode", "auth")
                .queryParam("login", login)
                .queryParam("passwd", password).open();
    }

    @Step("Админский логин")
    public void adminLogin() {
        login("gomer", "kfVDy6mgEP9zyNT");
    }

    @Step("Логин копирайтер/ретушер/фотограф")
    public void outstaffLogin() {
        login("gomer", "kfVDy6mgEP9zyNT");
    }

    @Step("Колл-центр логин")
    public void callCenterLogin() {
        login("gomer", "kfVDy6mgEP9zyNT");
    }

    @Step("Разлогин")
    public void logoff() {
        urlSteps.logout().queryParam("mode", "logout")
                .queryParam("global", "0")
                .queryParam("yu", getDriver().manage().getCookieNamed("yandexuid").getValue()).open();
    }
}
