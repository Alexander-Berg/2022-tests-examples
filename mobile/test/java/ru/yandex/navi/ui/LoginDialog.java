package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

import java.time.Duration;

public class LoginDialog extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Войти\")")
    @iOSXCUITFindBy(accessibility = "Выберите аккаунт")
    public MobileElement buttonLogin;

    private LoginDialog() {
        super();
        setView(buttonLogin);
    }

    public static LoginDialog getVisible() {
        LoginDialog dialog = new LoginDialog();
        dialog.checkVisible(Duration.ofSeconds(5));
        return dialog;
    }

    public void longTapOnAccount(String account) {
        user.longTap("Аккаунт: " + account, user.findElementByText(account).getCenter());
    }
}
