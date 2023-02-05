package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.HowToUseLocators;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import ru.yandex.navi.tf.MobileUser;

import java.time.Duration;

import static io.appium.java_client.pagefactory.LocatorGroupStrategy.ALL_POSSIBLE;

public final class IntroScreen extends BaseScreen {
    @iOSXCUITFindBy(accessibility = "intro_page_title")
    @AndroidFindBy(id = "intro_page_title")
    private MobileElement titleText;

    @iOSXCUITFindBy(accessibility = "button_action")
    @AndroidFindBy(id = "button_primary_action")
    private MobileElement primaryButton;

    @iOSXCUITFindBy(accessibility = "button_cancel")
    @AndroidFindBy(id = "button_secondary_action")
    private MobileElement secondaryButton;

    @iOSXCUITFindBy(accessibility = "button_close")
    @AndroidFindBy(id = "button_close")
    private MobileElement buttonClose;

    @HowToUseLocators(iOSXCUITAutomation = ALL_POSSIBLE)
    @iOSXCUITFindBy(accessibility = "Allow")
    @iOSXCUITFindBy(accessibility = "OK")
    @iOSXCUITFindBy(accessibility = "Разрешить")
    @iOSXCUITFindBy(accessibility = "При использовании приложения")
    @AndroidFindBy(id = "com.android.packageinstaller:id/permission_allow_button")
    private MobileElement permissionAllowButton;

    @HowToUseLocators(iOSXCUITAutomation = ALL_POSSIBLE)
    @iOSXCUITFindBy(accessibility = "Don’t Allow")
    @iOSXCUITFindBy(accessibility = "Запретить")
    @AndroidFindBy(id = "com.android.packageinstaller:id/permission_deny_button")
    private MobileElement permissionDenyButton;

    public IntroScreen() {
        super();
        setView(titleText);
    }

    public static IntroScreen getVisible() {
        IntroScreen screen = new IntroScreen();
        screen.checkVisible(Duration.ofSeconds(5));
        return screen;
    }

    public void clickAction() {
        user.clicks(MobileUser.isDisplayed(primaryButton)
                ? primaryButton
                : secondaryButton);
    }

    public boolean skip() {
        return skip(true);
    }

    @Step("Skip intro grantPermissions={grantPermissions}")
    public boolean skip(boolean grantPermissions) {
        final MobileElement permissionButton
            = grantPermissions ? permissionAllowButton : permissionDenyButton;
        final MobileElement[] buttons = {buttonClose, secondaryButton, primaryButton};

        // Wait for first intro screen...
        if (!isDisplayed(Duration.ofSeconds(3)))
            return false;

        boolean skipped = false;
        while (true) {
            MobileElement button = null;
            if (isDisplayed())
                button = findDisplayed(buttons);
            if (button == null && MobileUser.isDisplayed(permissionButton))
                button = permissionButton;
            if (button == null)
                break;

            user.clicks(button);
            skipped = true;
            user.waitFor(Duration.ofSeconds(1));
        }

        return skipped;
    }

    private MobileElement findDisplayed(MobileElement[] elements) {
        for (MobileElement element : elements) {
            if (MobileUser.isDisplayed(element))
                return element;
        }
        return null;
    }
}
