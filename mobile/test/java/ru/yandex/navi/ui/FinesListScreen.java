package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

import java.time.Duration;

public final class FinesListScreen extends BaseScreen {
    @AndroidFindBy(id = "finesList")
    @iOSXCUITFindBy(accessibility = "Штрафы")
    private MobileElement view;

    @AndroidFindBy(id = "fineContent")
    @iOSXCUITFindBy(className = "XCUIElementTypeCell")
    private MobileElement fineItem;

    private FinesListScreen() {
        super();
        setView(view);
    }

    public static FinesListScreen getVisible() {
        FinesListScreen screen = new FinesListScreen();
        screen.checkVisible(Duration.ofSeconds(10));
        return screen;
    }

    public void clickFine() {
        user.shouldSee(fineItem, Duration.ofSeconds(20));
        user.clicks(fineItem);
    }
}
