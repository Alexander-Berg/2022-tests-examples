package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.HowToUseLocators;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;

import java.util.Arrays;

import static io.appium.java_client.pagefactory.LocatorGroupStrategy.ALL_POSSIBLE;

public final class PermissionAlert extends BaseScreen {
    @AndroidFindBy(id = "com.android.packageinstaller:id/permission_message")
    @iOSXCUITFindBy(className = "XCUIElementTypeAlert")
    private MobileElement message;
    private final String[] title;

    private static final String[] TITLE_ACCESS_LOCATION = {
        "доступ к данным о местоположении устройства", "доступ к\u00a0Вашей геопозиции"
    };

    private static final String[] TITLE_RECORD_AUDIO = {
        "запись аудио", "записывать аудио", "доступ к микрофону"
    };

    private static final String[] TITLE_SEND_NOTIFICATION = {"Send You Notifications"};

    @HowToUseLocators(iOSXCUITAutomation = ALL_POSSIBLE)
    @iOSXCUITFindBy(accessibility = "Allow")
    @iOSXCUITFindBy(accessibility = "OK")
    @iOSXCUITFindBy(accessibility = "Разрешить")
    @iOSXCUITFindBy(accessibility = "При использовании приложения")
    @AndroidFindBy(id = "com.android.packageinstaller:id/permission_allow_button")
    private MobileElement permissionAllowButton;

    private PermissionAlert(String[] title) {
        super();
        this.title = title;
        setView(message);
    }

    private static PermissionAlert getVisible(String[] title) {
        PermissionAlert alert = new PermissionAlert(title);
        alert.checkVisible();
        return alert;
    }

    public static PermissionAlert getVisibleAccessLocationAlert() {
        return getVisible(TITLE_ACCESS_LOCATION);
    }

    public static PermissionAlert getVisibleRecordAudioAlert() {
        return getVisible(TITLE_RECORD_AUDIO);
    }

    public static PermissionAlert getVisibleSendNotificationAlert() {
        return getVisible(TITLE_SEND_NOTIFICATION);
    }

    @Override
    public boolean isDisplayed() {
        if (!super.isDisplayed())
            return false;
        return isTitleValid();
    }

    @Step("Нажать 'Разрешить'")
    public void clickAllow() {
        user.clicks(permissionAllowButton);
    }

    private boolean isTitleValid() {
        final String messageText = message.getText();
        return Arrays.stream(title).anyMatch(messageText::contains);
    }
}
