package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public class MastercardLiteMenu extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Скидки и предложения по картам Mastercard\")")
    @iOSXCUITFindBy(accessibility = "Скидки и предложения по картам Mastercard")
    private MobileElement view;

    private MastercardLiteMenu() {
        super();
        setView(view);
    }

    public static MastercardLiteMenu getVisible() {
        MastercardLiteMenu menu = new MastercardLiteMenu();
        menu.checkVisible();
        return menu;
    }
}
