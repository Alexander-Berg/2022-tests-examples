package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import ru.yandex.navi.tf.Direction;

public class BaseSettingsScreen extends BaseScreen {
    private final String title;

    @AndroidFindBy(id = "navigation_bar_title")
    @iOSXCUITFindBy(className = "XCUIElementTypeNavigationBar")
    private MobileElement navigationBarTitle;

    @AndroidFindBy(id = "navigation_bar_back_button")
    @iOSXCUITFindBy(accessibility = "Назад")
    private MobileElement backButton;

    BaseSettingsScreen(String title) {
        super();
        this.title = title;
    }

    public static BaseSettingsScreen getVisible(String title) {
        BaseSettingsScreen screen = new BaseSettingsScreen(title);
        screen.checkVisible();
        return screen;
    }

    @Override
    public boolean isDisplayed() {
        return navigationBarTitle.getText().equals(title);
    }

    @Step("Нажать BACK")
    public void clickBack() {
        user.clicks(backButton);
    }

    public BaseSettingsScreen click(String item) {
        return click(item, Direction.NONE);
    }

    @Step("Тапнуть пункт '{item}'")
    public BaseSettingsScreen click(String item, Direction direction) {
        user.clicks(item, direction);
        return this;
    }
}
