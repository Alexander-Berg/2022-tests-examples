package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public final class ParkingAutosScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Автомобили\")")
    @iOSXCUITFindBy(accessibility = "Автомобили")
    private MobileElement view;

    private ParkingAutosScreen() {
        super();
        setView(view);
    }

    public static ParkingAutosScreen getVisible() {
        ParkingAutosScreen screen = new ParkingAutosScreen();
        screen.checkVisible();
        return screen;
    }

    public ParkingNewAutoScreen clickNewAuto() {
        user.clicks("Новый автомобиль");
        return ParkingNewAutoScreen.getVisible();
    }
}
