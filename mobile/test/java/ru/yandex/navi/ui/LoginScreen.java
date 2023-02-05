package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

import java.time.Duration;

public final class LoginScreen extends BaseScreen {
    @AndroidFindBy(id = "edit_login")
    @iOSXCUITFindBy(accessibility = "edit_login")
    private MobileElement textLogin;

    @AndroidFindBy(id = "button_next")
    @iOSXCUITFindBy(accessibility = "button_next")
    private MobileElement buttonNext;

    private LoginScreen() {
        super();
        setView(textLogin);
    }

    public static LoginScreen getVisible() {
        LoginScreen screen = new LoginScreen();
        screen.checkVisible(Duration.ofSeconds(5));
        return screen;
    }

    public LoginScreen enterLogin(String text) {
        user.types(textLogin, text);
        return this;
    }

    public LoginPasswordScreen clickNext() {
        user.clicks(buttonNext);
        return LoginPasswordScreen.getVisible();
    }
}
