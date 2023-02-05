package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

import java.util.List;

public final class SearchHistoryScreen extends BaseScreen {
    @AndroidFindBy(id = "searchbar_main")
    @iOSXCUITFindBy(accessibility = "SearchBar")
    private MobileElement searchBar;

    @AndroidFindBy(id = "textview_search")
    public List<MobileElement> items;

    private SearchHistoryScreen() {
        super();
        setView(searchBar);
    }

    public static SearchHistoryScreen getVisible() {
        SearchHistoryScreen screen = new SearchHistoryScreen();
        screen.checkVisible();
        return screen;
    }

    public SearchHistoryScreen clickFirstItem() {
        user.clicks(items.get(0));
        return this;
    }

    public GeoCard expectGeoCard() {
        return GeoCard.getVisible();
    }
}
