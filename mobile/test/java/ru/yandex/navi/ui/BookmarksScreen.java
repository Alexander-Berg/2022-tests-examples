package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.HowToUseLocators;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.junit.Assert;

import java.time.Duration;
import java.util.List;

import static io.appium.java_client.pagefactory.LocatorGroupStrategy.ALL_POSSIBLE;

public class BookmarksScreen extends BaseScreen {
    @AndroidFindBy(id = "bookmarks_screen")
    @iOSXCUITFindBy(accessibility = "BookmarksView")
    private MobileElement view;

    @AndroidFindBy(id = "toolbar_edit_button")
    private MobileElement edit;

    @HowToUseLocators(androidAutomation = ALL_POSSIBLE)
    @AndroidFindBy(id = "point_cell_background")
    @AndroidFindBy(id = "place_cell_background")
    private List<MobileElement> rows;

    private BookmarksScreen() {
        super();
        setView(view);
    }

    public static BookmarksScreen getVisible() {
        BookmarksScreen screen = new BookmarksScreen();
        screen.checkVisible(Duration.ofSeconds(2));
        return screen;
    }

    @Step("Нажать 'Добавить адрес'")
    public final SearchScreen addAddress() {
        user.clicks("Добавить адрес");
        return SearchScreen.getVisible();
    }

    @Step("Нажать 'Дом'")
    public final OverviewScreen clickHomeExpectOverview() {
        user.clicks("Дом");
        return OverviewScreen.getVisible();
    }

    @Step("Нажать на значок Карандаша")
    public EditBookmarksScreen clickEdit() {
        user.clicks(edit);
        return EditBookmarksScreen.getVisible();
    }

    @Step("Нажать на значок Карандаша")
    public EditBookmarksScreen clickEditExpect(String title) {
        user.clicks(edit);
        return EditBookmarksScreen.getVisible(title);
    }

    @Step("Нажать 'Новый список'")
    public InputDialog clickNewList() {
        user.clicks("Новый список");
        return InputDialog.withTitle("Новый список");
    }

    @Step("Нажать на '{item}'")
    public BookmarksScreen clickItem(String item) {
        user.clicks(item);
        return this;
    }

    @Step("Нажать 'Недавние'")
    public BookmarksHistoryScreen clickRecent() {
        user.clicks("Недавние");
        return BookmarksHistoryScreen.getVisible();
    }

    public void checkButtonAdd(String item, boolean value) {
        for (MobileElement row : rows) {
            if (user.findElementsByTextFrom(row, item).isEmpty())
                continue;
            List<MobileElement> items = user.findElementsByTextFrom(row, "Добавить");
            Assert.assertEquals(items.isEmpty(), !value);
        }
    }
}
