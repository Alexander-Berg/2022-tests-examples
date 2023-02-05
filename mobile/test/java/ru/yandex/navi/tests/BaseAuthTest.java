package ru.yandex.navi.tests;

import io.qameta.allure.Step;
import ru.yandex.navi.Credentials;
import ru.yandex.navi.ui.Dialog;
import ru.yandex.navi.ui.LoginDialog;
import ru.yandex.navi.ui.LoginPasswordScreen;
import ru.yandex.navi.ui.LoginScreen;
import ru.yandex.navi.ui.MenuScreen;
import ru.yandex.navi.ui.TabBar;

import java.time.Duration;

class BaseAuthTest extends BaseTest {
    @Step("Авторизоваться тестовым аккаунтом '{account}'")
    void login(TabBar tabBar, Credentials credentials) {
        doLogin(tabBar, credentials, false).close();
    }

    @Step("Авторизоваться тестовым аккаунтом '{account}'")
    MenuScreen loginInMenu(TabBar tabBar, Credentials credentials) {
        return doLogin(tabBar, credentials, false);
    }

    @Step("Авторизоваться тестовым аккаунтом '{account}'")
    void loginWithRotation(TabBar tabBar, Credentials credentials) {
        doLogin(tabBar, credentials, true).close();
    }

    private MenuScreen doLogin(TabBar tabBar, Credentials credentials, boolean withRotation) {
        MenuScreen menuScreen = tabBar.clickMenu();

        LoginScreen loginScreen = menuScreen.clickLogin();

        if (withRotation)
            rotateAndReturn();

        LoginPasswordScreen passwordScreen =
            loginScreen.enterLogin(credentials.userName).clickNext();

        if (withRotation)
            rotateAndReturn();

        passwordScreen.enterPassword(credentials.password).clickNext();
        user.shouldSee(menuScreen, Duration.ofSeconds(3));

        return menuScreen;
    }

    @Step("Выйти из аккаунта")
    final void logout(TabBar tabBar) {
        MenuScreen menuScreen = tabBar.clickMenu();
        doLogout(menuScreen);
        menuScreen.close();
    }

    @Step("Выйти из аккаунта")
    final void logout(MenuScreen menuScreen) {
        doLogout(menuScreen);
    }

    private void doLogout(MenuScreen menuScreen) {
        menuScreen.clickLogout();
        user.shouldSeeInScrollable(menuScreen.buttonLogin);
    }

    final void removeAccount(TabBar tabBar, String userName) {
        MenuScreen menuScreen = tabBar.clickMenu();
        LoginDialog loginDialog = menuScreen.clickLoginExpectLoginDialog();
        loginDialog.longTapOnAccount(userName);

        Dialog.withTitle("Вы хотите удалить аккаунт?")
                .clickAt("Удалить");

        LoginScreen.getVisible();
    }

    @Step("Авторизоваться тестовым аккаунтом '{credentials.userName}'")
    final void login(LoginScreen loginScreen, Credentials credentials) {
        loginScreen
            .enterLogin(credentials.userName)
            .clickNext()
            .enterPassword(credentials.password)
            .clickNext();
    }
}
