package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

import java.time.Duration;

public class FinesScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Штрафы\")")
    @iOSXCUITFindBy(accessibility = "Штрафы")
    private MobileElement view;

    @AndroidFindBy(id = "edit_text")
    @iOSXCUITFindBy(accessibility = "Номер СТС")
    private MobileElement editSts;

    @AndroidFindBy(uiAutomator = ".text(\"Проверить\")")
    @iOSXCUITFindBy(accessibility = "Проверить")
    private MobileElement checkButton;

    private FinesScreen() {
        super();
        setView(view);
    }

    public static FinesScreen getVisible() {
        FinesScreen screen = new FinesScreen();
        screen.checkVisible(Duration.ofSeconds(5));
        return screen;
    }

    public FinesScreen typeSts(String value) {
        editSts.click();
        user.types(editSts, value);
        return this;
    }

    public void clickCheck() {
        checkButton.click();
    }
}
