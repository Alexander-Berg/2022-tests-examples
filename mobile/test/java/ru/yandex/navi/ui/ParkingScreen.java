package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public class ParkingScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Настройки парковок\")")
    @iOSXCUITFindBy(accessibility = "Настройки парковок")
    private MobileElement view;

    private ParkingScreen() {
        super();
        setView(view);
    }

    public static ParkingScreen getVisible() {
        ParkingScreen screen = new ParkingScreen();
        screen.checkVisible();
        return screen;
    }

    public ParkingAutosScreen clickAutos() {
        user.clicks("Автомобили");
        return ParkingAutosScreen.getVisible();
    }
}
