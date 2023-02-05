package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public final class AlicePopup extends BaseScreen {
    @AndroidFindBy(id = "alice_widget")
    @iOSXCUITFindBy(accessibility = "Как вам помочь?")
    private MobileElement view;

    private AlicePopup() {
        super();
        setView(view);
    }

    public static AlicePopup getVisible() {
        AlicePopup screen = new AlicePopup();
        screen.checkVisible();
        return screen;
    }
}
