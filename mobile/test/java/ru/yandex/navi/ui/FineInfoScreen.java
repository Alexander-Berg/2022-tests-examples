package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

import java.time.Duration;

public final class FineInfoScreen extends BaseScreen {
    @AndroidFindBy(id = "details")
    @iOSXCUITFindBy(accessibility = "Штраф")
    private MobileElement view;

    @AndroidFindBy(id = "edit_text")
    @iOSXCUITFindBy(className = "XCUIElementTypeTextView")
    private MobileElement payer;

    @AndroidFindBy(uiAutomator = ".text(\"Заплатить\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeButton[`name BEGINSWITH \"Заплатить\"`]")
    private MobileElement buttonPay;

    private FineInfoScreen() {
        super();
        setView(view);
    }

    public static FineInfoScreen getVisible() {
        FineInfoScreen screen = new FineInfoScreen();
        screen.checkVisible(Duration.ofSeconds(5));
        return screen;
    }

    public FineInfoScreen inputPayer(String value) {
        payer.click();
        user.types(payer, value);
        user.hideKeyboard();
        return this;
    }

    public void clickPay() {
        user.clicks(buttonPay);
    }
}
