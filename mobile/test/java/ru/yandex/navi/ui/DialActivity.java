package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;

import java.time.Duration;

public final class DialActivity extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Создать контакт\")")
    private MobileElement view;

    private DialActivity() {
        super();
        setView(view);
    }

    public static DialActivity getVisible() {
        final DialActivity dialActivity = new DialActivity();
        dialActivity.checkVisible(Duration.ofSeconds(3));
        return dialActivity;
    }

    public void closeDialActivity() {
        for (int i = 0; i < 3; ++i)
            user.navigateBack();
    }
}
