package ru.yandex.navi.ui;

import java.time.Duration;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public final class PlusHomeScreen extends BaseScreen {
    // view with user's Plus score or "Плюс" text if user hasn't bought Plus yet
    @AndroidFindBy(id = "plus_cashback_amount")
    @iOSXCUITFindBy(accessibility = "plus_cashback_amount")
    private MobileElement cashbackAmount;

    private PlusHomeScreen() {
        super();
        setView(cashbackAmount);
    }

    public static PlusHomeScreen getVisible() {
        PlusHomeScreen screen = new PlusHomeScreen();
        screen.checkVisible(Duration.ofSeconds(5));
        return screen;
    }
}
