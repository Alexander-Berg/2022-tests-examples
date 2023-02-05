package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public class FullscreenInputDialog extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Комментарий\")")
    @iOSXCUITFindBy(accessibility = "Комментарий")
    private MobileElement title;

    @AndroidFindBy(uiAutomator = ".text(\"Готово\")")
    @iOSXCUITFindBy(accessibility = "Готово")
    private MobileElement buttonDone;

    @AndroidFindBy(id = "fullscreen_input_edit_text")
    @iOSXCUITFindBy(className = "XCUIElementTypeTextView")
    private MobileElement edit;

    private FullscreenInputDialog() {
        super();
        setView(title);
    }

    public static FullscreenInputDialog getVisible() {
        FullscreenInputDialog screen = new FullscreenInputDialog();
        screen.checkVisible();
        return screen;
    }

    public final FullscreenInputDialog enterText(String text) {
        user.types(edit, text);
        return this;
    }

    public final void clickDone() {
        user.clicks(buttonDone);
    }
}
