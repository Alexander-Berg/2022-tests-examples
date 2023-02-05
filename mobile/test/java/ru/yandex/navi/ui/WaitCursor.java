package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.junit.Assert;

public final class WaitCursor extends BaseScreen {
    @AndroidFindBy(id = "view_waitcursor_root")
    @iOSXCUITFindBy(accessibility = "Поиск маршрута...")
    private MobileElement view;

    @AndroidFindBy(id = "text_waitcursor_message")
    private MobileElement title;

    private WaitCursor() {
        super();
        setView(view);
    }

    public static WaitCursor getVisible() {
        WaitCursor screen = new WaitCursor();
        screen.checkVisible();
        return screen;
    }

    @Step("Тапнуть 'Закрыть'")
    public void clickClose() {
        user.clicks("Закрыть");
    }

    public void checkTitle(String value) {
        Assert.assertEquals(value, title.getText());
    }

    public void hasButton(String value) {
        user.shouldSee(value);
    }
}
