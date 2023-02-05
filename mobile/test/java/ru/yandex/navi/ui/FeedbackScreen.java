package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import ru.yandex.navi.tf.Direction;

public final class FeedbackScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Обратная связь\")")
    @iOSXCUITFindBy(accessibility = "Обратная связь")
    private MobileElement view;

    private FeedbackScreen() {
        super();
        setView(view);
    }

    public static FeedbackScreen getVisible() {
        FeedbackScreen screen = new FeedbackScreen();
        screen.checkVisible();
        return screen;
    }

    @Step("Прокрутить экран 'Обратная связь' вниз")
    public FeedbackScreen scrollDown() {
        user.swipe(Direction.UP);
        return this;
    }

    @Step("Прокрутить экран 'Обратная связь' вверх")
    public void scrollUp() {
        user.swipe(Direction.DOWN);
    }
}
