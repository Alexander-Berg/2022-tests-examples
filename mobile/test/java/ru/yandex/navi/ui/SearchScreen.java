package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import ru.yandex.navi.SearchCategory;
import ru.yandex.navi.tf.MobileUser;
import ru.yandex.navi.tf.NoRetryException;

import java.time.Duration;

public abstract class SearchScreen extends BaseScreen {
    SearchScreen() {
        super();
    }

    public static SearchScreen getVisible() {
        final SearchScreen searchV1 = new SearchScreenV1();
        final SearchScreen searchV2 = new SearchScreenV2();
        MobileUser.getUser().shouldSee(() -> searchV1.isDisplayed() || searchV2.isDisplayed(),
            Duration.ofSeconds(3));
        return searchV1.isDisplayed() ? searchV1 : searchV2;
    }

    public final SearchSuggestScreen searchFor(String text) {
        return clickSearch().searchFor(text);
    }

    public final void checkCategories(String... categories) {
        user.shouldSeeAll(categories);
    }

    public final SearchSuggestScreen clickSearch() {
        user.clicks(getSearchField());
        return SearchSuggestScreen.getVisible();
    }

    @Step("Тапнуть на категорию 'Где поесть'")
    public final GeoCard clickWhereToEatExpectGeoCard() {
        doClick(SearchCategory.WHERE_TO_EAT);
        return GeoCard.getVisible(Duration.ofSeconds(30));
    }

    @Step("Тапнуть на категорию '{category}")
    public final void click(SearchCategory category) {
        doClick(category);
    }

    private void doClick(SearchCategory category) {
        user.clicks(category.toString());
    }

    @Step("Search for '{text}' and click first result")
    public final GeoCard searchAndClickFirstItem(String text) {
        return searchFor(text).clickFirstItem().expectGeoCard();
    }

    @Step("Нажать на 'История'")
    public final SearchHistoryScreen clickHistory() {
        user.clicks(getHistory());
        return SearchHistoryScreen.getVisible();
    }

    protected abstract MobileElement getSearchField();
    protected abstract MobileElement getHistory();
}

final class SearchScreenV1 extends SearchScreen {
    @AndroidFindBy(id = "searchbar_main")
    @iOSXCUITFindBy(accessibility = "SearchBar")
    private MobileElement searchBar;

    @AndroidFindBy(id = "edittext_searchbar")
    @iOSXCUITFindBy(className = "XCUIElementTypeTextField")
    private MobileElement searchField;

    @AndroidFindBy(uiAutomator = ".text(\"История\")")
    @iOSXCUITFindBy(accessibility = "История")
    private MobileElement history;

    SearchScreenV1() {
        super();
        setView(searchBar);
    }

    @Override
    protected MobileElement getSearchField() {
        return searchField;
    }

    @Override
    protected MobileElement getHistory() {
        return history;
    }
}

final class SearchScreenV2 extends SearchScreen {
    @AndroidFindBy(id = "search_line_edit_text_container")
    private MobileElement searchBar;

    @AndroidFindBy(id = "search_line_edit_text")
    private MobileElement searchField;

    SearchScreenV2() {
        super();
        setView(searchBar);
    }

    @Override
    protected MobileElement getSearchField() {
        return searchField;
    }

    @Override
    protected MobileElement getHistory() {
        throw new NoRetryException("Not implemented: SearchScreenV2.getHistory");
    }
}
