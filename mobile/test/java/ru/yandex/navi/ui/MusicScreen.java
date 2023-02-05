package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public final class MusicScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Яндекс Музыка\")")
    @iOSXCUITFindBy(accessibility = "Яндекс Музыка")
    private MobileElement view;

    private MusicScreen() {
        super();
        setView(view);
    }

    public static MusicScreen getVisible() {
        MusicScreen screen = new MusicScreen();
        screen.checkVisible();
        return screen;
    }
}
