package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import ru.yandex.navi.tf.Direction;

import java.util.List;

public final class SettingsScreen extends BaseSettingsScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Звуковое сопровождение\")")
    @iOSXCUITFindBy(iOSClassChain =
        "**/XCUIElementTypeStaticText[`name = \"Звуковое сопровождение\"`]")
    public MobileElement sounds;

    @AndroidFindBy(uiAutomator = ".textStartsWith(\"Зафиксировать подсказки\")")
    @iOSXCUITFindBy(iOSClassChain =
        "**/XCUIElementTypeStaticText[`name BEGINSWITH \"Зафиксировать подсказки\"`]")
    public MobileElement fixManeuvers;

    @AndroidFindBy(uiAutomator = ".text(\"Об ограничениях скорости\")")
    @iOSXCUITFindBy(iOSClassChain =
        "**/XCUIElementTypeStaticText[`name = \"Об ограничениях скорости\"`]")
    public MobileElement speedLimit;

    @AndroidFindBy(uiAutomator = ".text(\"Курсор\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name = \"Курсор\"`]")
    private MobileElement cursor;

    @AndroidFindBy(uiAutomator = ".text(\"Голос\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name = \"Голос\"`]")
    public MobileElement voice;

    @AndroidFindBy(uiAutomator = ".text(\"Алиса\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name = \"Алиса\"`]")
    public MobileElement alice;

    @AndroidFindBy(uiAutomator = ".text(\"Синхронизировать настройки\")")
    @iOSXCUITFindBy(iOSClassChain =
        "**/XCUIElementTypeStaticText[`name = \"Синхронизировать настройки\"`]")
    public MobileElement syncSettings;

    @AndroidFindBy(id = "menu_setting_item_switch")
    public List<MobileElement> checkboxes;

    @AndroidFindBy(id = "navbar_close_button")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeNavigationBar/XCUIElementTypeButton")
    private MobileElement buttonClose;

    public SettingsScreen() {
        this("Настройки");
    }

    private SettingsScreen(String title) {
        super(title);
    }

    public static SettingsScreen getVisible() {
        return getVisible("Настройки");
    }

    public static SettingsScreen getVisible(String title) {
        SettingsScreen screen = new SettingsScreen(title);
        screen.checkVisible();
        return screen;
    }

    @Step("Перейти к {items}")
    public void click(String... items) {
        for (String item : items)
            user.clicks(item, Direction.DOWN);
    }

    @Step("Закрыть 'Настройки' тапом на 'X'")
    public void clickClose() {
        user.clicks(buttonClose);
    }

    @Step("Тапнуть 'Сохраненные данные'")
    public SavedDataScreen clickSavedData() {
        user.clicks("Сохраненные данные", Direction.DOWN);
        return SavedDataScreen.getVisible();
    }

    public BaseSettingsScreen clickCursor() {
        user.clicksInScrollable(cursor);
        return BaseSettingsScreen.getVisible("Курсор");
    }

    public BaseSettingsScreen clickVoice() {
        user.clicksInScrollable(voice);
        return BaseSettingsScreen.getVisible("Голос");
    }
}
