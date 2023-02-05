package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.junit.Assert;

import java.time.Duration;
import java.util.List;

public class SearchSuggestScreen extends BaseScreen {
    @AndroidFindBy(id = "searchbar_main")
    @iOSXCUITFindBy(accessibility = "SearchBar")
    private MobileElement searchBar;

    @AndroidFindBy(id = "edittext_searchbar")
    @iOSXCUITFindBy(className = "XCUIElementTypeTextField")
    private MobileElement searchField;

    @AndroidFindBy(id = "textview_searchmessage_title")
    private MobileElement searchError;

    @AndroidFindBy(id = "item_searchsuggest")
    @iOSXCUITFindBy(accessibility = "SuggestTableViewCell")
    public List<MobileElement> items;

    private SearchSuggestScreen() {
        super();
        setView(searchBar);
    }

    public static SearchSuggestScreen getVisible() {
        SearchSuggestScreen screen = new SearchSuggestScreen();
        screen.checkVisible();
        return screen;
    }

    SearchSuggestScreen searchFor(String text) {
        typeText(text);
        user.shouldSeeSuggest(items);
        return this;
    }

    @Step("Ввести текст '{text}'")
    public SearchSuggestScreen typeText(String text) {
        user.types(searchField, text);
        return this;
    }

    public SearchSuggestScreen clickFirstItem() {
        doClickFirstItem(null);
        return this;
    }

    public SearchSuggestScreen clickFirstItem(int count) {
        for (int i = 0; i < count; ++i) {
            if (i > 0 && items.isEmpty())
                break;
            doClickFirstItem(null);
        }
        return this;
    }

    SearchSuggestScreen clickFirstItem(String text) {
        doClickFirstItem(text);
        return this;
    }

    public GeoCard expectGeoCard() {
        return GeoCard.getVisible();
    }

    private void doClickFirstItem(String text) {
        if (text != null) {
            final List<MobileElement> elements = user.findElementsByText(text);
            if (elements.size() > 1) {  // skip text in search bar
                user.clicks(elements.get(1));
                return;
            }
        }

        user.clicks(items.get(0));
    }

    public void expectError(String error, Duration timeout) {
        user.shouldSee(searchError, timeout);
        Assert.assertEquals(error, searchError.getText());
    }
}
