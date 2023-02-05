package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.ScreenOrientation;

import java.util.List;

public class TabBar extends BaseScreen {
    static class Button {
        final String name;
        private final int index4;  // iOS: Tabbar without Music
        private final int index5;

        Button(String name, int index4, int index5) {
            this.name = name;
            this.index4 = index4;
            this.index5 = index5;
        }

        int getIndexForSize(int size) {
            if (size == 4)
                return index4;
            if (size == 5)
                return index5;
            return -1;
        }
    }

    public static final Button SEARCH = new Button("Поиск", 0, 0);
    private static final Button MAP = new Button("Карта", 1, 1);
    public static final Button OVERVIEW = new Button("Обзор", 1, 1);
    public static final Button MUSIC = new Button("Музыка", -1, 2);
    public static final Button BOOKMARKS = new Button("Мои места", 2, 3);
    public static final Button MENU = new Button("Меню", 3, 4);

    @AndroidFindBy(id = "tab_bar_items")
    @iOSXCUITFindBy(accessibility = "TabBar")
    private MobileElement tabBar;

    @AndroidFindBy(id = "tab_bar_item")
    @iOSXCUITFindBy(accessibility = "TabBarItemCell")
    private List<MobileElement> buttons;

    public TabBar() {
        super();
        setView(tabBar);
    }

    @Step("Тапнуть на 'Поиск' в таббаре")
    public SearchScreen clickSearch() {
        click(SEARCH);
        return SearchScreen.getVisible();
    }

    @Step("Click tabbar Map")
    public void clickMap() {
        click(MAP);
        MapScreen.getVisible();
    }

    @Step("Нажать на кнопку 'Мои места' в таббаре")
    public BookmarksScreen clickBookmarks() {
        click(BOOKMARKS);
        return BookmarksScreen.getVisible();
    }

    @Step("Тапнуть на кнопку 'Меню' в таббаре")
    public MenuScreen clickMenu() {
        click(MENU);
        return MenuScreen.getVisible();
    }

    @Step("Нажать на кнопку 'Музыка' в таббаре")
    public void clickMusic() {
        click(MUSIC);
        MusicScreen.getVisible();
    }

    private void click(Button button) {
        user.clicks(getButton(button));
    }

    MobileElement getButton(Button button) {
        final MobileElement[] buttons = this.buttons.toArray(new MobileElement[0]);

        int index = button.getIndexForSize(buttons.length);
        if (index >= 0 && user.getOrientation() == ScreenOrientation.LANDSCAPE)
            index = buttons.length - 1 - index;
        if (index < 0) {
            throw new NoSuchElementException(
                String.format("Can't find button '%s': size=%d", button.name, buttons.length));
        }

        return buttons[index];
    }

    MobileElement getElement() {
        return tabBar;
    }

    Rectangle getRect() {
        return tabBar.getRect();
    }
}
