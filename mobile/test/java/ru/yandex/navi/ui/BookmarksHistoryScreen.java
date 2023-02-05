package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;

import java.time.Duration;
import java.util.List;

public final class BookmarksHistoryScreen extends BaseScreen {
    @AndroidFindBy(id = "bookmarks_screen")
    @iOSXCUITFindBy(accessibility = "BookmarksView")
    private MobileElement view;

    @AndroidFindBy(id = "point_cell_background")
    @iOSXCUITFindBy(accessibility = "SuggestTableViewCell")
    private List<MobileElement> items;

    private BookmarksHistoryScreen() {
        super();
        setView(view);
    }

    public static BookmarksHistoryScreen getVisible() {
        BookmarksHistoryScreen screen = new BookmarksHistoryScreen();
        screen.checkVisible();
        return screen;
    }

    @Step("Тапнуть по любой строке с адресом")
    public void clickFirstItem() {
        user.shouldSee("history", items, Duration.ofSeconds(1));
        user.clicks(items.get(0));
    }
}
