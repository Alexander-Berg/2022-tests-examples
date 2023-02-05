package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public final class ParkingNewAutoScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Новое авто\")")
    @iOSXCUITFindBy(accessibility = "Новое авто")
    private MobileElement view;

    @AndroidFindBy(id = "etNumber")
    private MobileElement autoNumber;

    private ParkingNewAutoScreen() {
        super();
        setView(view);
    }

    public static ParkingNewAutoScreen getVisible() {
        ParkingNewAutoScreen screen = new ParkingNewAutoScreen();
        screen.checkVisible();
        return screen;
    }

    public ParkingNewAutoScreen enterAuto(String value) {
        user.types(autoNumber, value);
        return this;
    }
}
