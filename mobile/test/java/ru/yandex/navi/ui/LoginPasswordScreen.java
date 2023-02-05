package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import ru.yandex.navi.tf.Platform;

final class IosSavePasswordDialog extends BaseScreen {
    @iOSXCUITFindBy(iOSClassChain =
            "**/XCUIElementTypeStaticText[`name BEGINSWITH \"Хотите сохранить этот пароль\"`]")
    private MobileElement view;

    @iOSXCUITFindBy(accessibility = "Запретить")
    private MobileElement buttonDeny;

    IosSavePasswordDialog() {
        super();
        setView(view);
    }

    final void clickDeny() {
        user.clicks(buttonDeny);
    }
}

public final class LoginPasswordScreen extends BaseScreen {
    @AndroidFindBy(id = "edit_password")
    @iOSXCUITFindBy(accessibility = "edit_password")
    private MobileElement textPassword;

    @AndroidFindBy(id = "button_next")
    @iOSXCUITFindBy(accessibility = "button_next")
    private MobileElement buttonNext;

    private LoginPasswordScreen() {
        super();
        setView(textPassword);
    }

    public static LoginPasswordScreen getVisible() {
        LoginPasswordScreen screen = new LoginPasswordScreen();
        screen.checkVisible();
        return screen;
    }

    public LoginPasswordScreen enterPassword(String text) {
        user.types(textPassword, text);
        return this;
    }

    public void clickNext() {
        user.clicks(buttonNext);
        skipSavePasswordDialog();
    }

    private void skipSavePasswordDialog() {
        if (user.getPlatform() == Platform.iOS) {
            IosSavePasswordDialog savePasswordDialog = new IosSavePasswordDialog();
            if (savePasswordDialog.isDisplayed())
                savePasswordDialog.clickDeny();
        }
    }
}
