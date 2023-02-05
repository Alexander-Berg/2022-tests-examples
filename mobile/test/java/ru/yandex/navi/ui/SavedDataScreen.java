package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public final class SavedDataScreen extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Сохраненные данные\")")
    @iOSXCUITFindBy(accessibility = "Сохраненные данные")
    private MobileElement view;

    @AndroidFindBy(uiAutomator = ".text(\"Стереть карты\")")
    @iOSXCUITFindBy(accessibility = "Стереть карты")
    private MobileElement clearMaps;

    @AndroidFindBy(id = "menu_setting_item_right_text")
    @iOSXCUITFindBy(accessibility = "menu_setting_item_right_text")
    private MobileElement sizeOfMaps;

    private SavedDataScreen() {
        super();
        setView(view);
    }

    public static SavedDataScreen getVisible() {
        SavedDataScreen screen = new SavedDataScreen();
        screen.checkVisible();
        return screen;
    }

    public void clickClearSearchHistory(boolean confirm) {
        clearData("Очистить историю поиска", confirm);
    }

    public void clickClearRouteHistory(boolean confirm) {
        clearData("Очистить историю маршрутов", confirm);
    }

    public void clickClearCache(boolean confirm) {
        clearData("Очистить кеш", confirm);
    }

    private void clearData(String menuItem, boolean confirm) {
        user.clicks(menuItem);

        Dialog.withTitle(menuItem + "?").clickAt(confirm ? "Да" : "Нет");
    }

    public void clickClearMaps() {
        user.clicks(clearMaps);
    }

    public int getSizeOfMaps() {
        return parseSize(sizeOfMaps.getText());
    }

    private static int parseSize(String text) {
        final String NBSP = "\u00a0";
        final String[] tokens = text.split(NBSP);
        if (tokens.length != 2)
            throw new AssertionError("Unparsable value '" + text + "'");

        final int size = Integer.parseInt(tokens[0]);
        final int factor;
        switch (tokens[1]) {
            case "кБ": factor = 1024; break;
            case "МБ": factor = 1024 * 1024; break;
            default: throw new AssertionError("Unexpected factor in '" + text + "'");
        }
        return size * factor;
    }
}
