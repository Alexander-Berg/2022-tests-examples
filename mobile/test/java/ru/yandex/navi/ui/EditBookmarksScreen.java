package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.HowToUseLocators;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.junit.Assert;
import ru.yandex.navi.tf.Direction;

import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.NoSuchElementException;

import static io.appium.java_client.pagefactory.LocatorGroupStrategy.ALL_POSSIBLE;

public final class EditBookmarksScreen extends BaseScreen {
    @AndroidFindBy(id = "bookmarks_screen")
    @iOSXCUITFindBy(accessibility = "BookmarksView")
    private MobileElement view;

    @AndroidFindBy(id = "navigation_bar_title")
    private MobileElement navigationBar;

    @AndroidFindBy(id = "toolbar_remove_button")
    private MobileElement remove;

    @HowToUseLocators(androidAutomation = ALL_POSSIBLE)
    @AndroidFindBy(id = "point_cell_background")
    @AndroidFindBy(id = "place_cell_background")
    private List<MobileElement> rows;

    @AndroidFindBy(id = "point_cell_title")
    private List<MobileElement> titles;

    @AndroidFindBy(id = "point_cell_handle")
    public List<MobileElement> handles;

    private EditBookmarksScreen() {
        super();
        setView(view);
    }

    private EditBookmarksScreen(String title) {
        super();
        setView(navigationBar);
        Assert.assertEquals(title, navigationBar.getText());
    }

    public static EditBookmarksScreen getVisible() {
        EditBookmarksScreen screen = new EditBookmarksScreen();
        screen.checkVisible();
        return screen;
    }

    public static EditBookmarksScreen getVisible(String title) {
        EditBookmarksScreen screen = new EditBookmarksScreen(title);
        screen.checkVisible();
        return screen;
    }

    @Step("Нажать на checkbox '{item}'")
    public EditBookmarksScreen clickCheckbox(String item) {
        for (MobileElement row : rows) {
            if (user.findElementsByTextFrom(row, item).isEmpty())
                continue;
            final MobileElement checkbox = row.findElementByXPath("//android.widget.CheckBox");
            user.clicks(checkbox);
            return this;
        }
        throw new NoSuchElementException(String.format("Cannot find checkbox '%s'", item));
    }

    @Step("Нажать на значок Корзины, подтвердить удаление")
    public void clickRemove() {
        user.clicks(remove);

        Dialog.withTitle("Выбранные объекты (1) будут удалены навсегда.").clickAt("Удалить");
    }

    @Step("Закрыть свайпом вниз")
    public void closeBySwipe() {
        user.swipe(navigationBar, Direction.DOWN);
    }

    public ArrayList<String> getTitles() {
        ArrayList<String> result = new ArrayList<>();
        for (MobileElement title : titles)
            result.add(title.getText());
        return result;
    }
}
