package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.experimental.Accessors;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.config.RealtyWebConfig;

/**
 * Created by vicdev on 29.06.17.
 */
@Accessors(chain = true)
public class PassportSteps extends WebDriverSteps {

    @Inject
    private WebDriverManager wm;

    @Inject
    private RealtyWebConfig config;

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

    @Step("Разлогин")
    public void logoff() {
        urlSteps.logout().queryParam("mode", "logout")
                .queryParam("global", "0")
                .queryParam("yu", wm.getDriver().manage().getCookieNamed("yandexuid").getValue()).open();
    }
}
