package ru.yandex.general.events;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.general.consts.Events.SEARCH;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SEARCH;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.element.SearchBar.FIND;
import static ru.yandex.general.page.ListingPage.CONDITION;
import static ru.yandex.general.page.ListingPage.PRICE;
import static ru.yandex.general.page.ListingPage.PRICE_FROM;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SEARCH)
@DisplayName("Отправка «search», обновление queryId")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class SearchQueryIdChangeTest {

    private static final String SEARCH_TEXT = "ноутбук macbook";

    private String queryId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private EventSteps eventSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.waitSomething(3, TimeUnit.SECONDS);
        eventSteps.clearHar();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при рефреше главной")
    public void shouldSeeUpdatedQueryIdByRefreshHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();
        basePageSteps.refresh();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при поиске с главной")
    public void shouldSeeUpdatedQueryIdBySearchFromHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().searchBar().fillSearchInput(SEARCH_TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при переходе с главной на категории из предложенных")
    public void shouldSeeUpdatedQueryIdGoToMainCategoryFromHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().homeMainCategories().link("Электроника").click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при переходе с главной на категории из списка сбоку")
    public void shouldSeeUpdatedQueryIdGoToSideCategoryFromHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().sidebarCategories().link("Компьютерная техника").click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при переходе с главной на категории в футере")
    public void shouldSeeUpdatedQueryIdGoToFooterCategoryFromHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().footer().category("Работа").click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Ignore("CLASSFRONT-1557")
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при переходе с главной на город в футере")
    public void shouldSeeUpdatedQueryIdGoToFooterCityFromHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().footer().city("Санкт-Петербург").click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при рефреше листинга категории")
    public void shouldSeeUpdatedQueryIdByRefreshCategory() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.refresh();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при поиске с листинга категории")
    public void shouldSeeUpdatedQueryIdBySearchFromCategoryListing() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().searchBar().input().sendKeys(SEARCH_TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при переходе на главную с листинга категории")
    public void shouldSeeUpdatedQueryIdFromCategoryListingToHomepage() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().oLogo().click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при переходе на категорию по ХК с листинга категории")
    public void shouldSeeUpdatedQueryIdFromCategoryListingToAnotherCategory() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().breadcrumbsItem("Компьютерная техника").click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при смене сортировки")
    public void shouldSeeUpdatedQueryIdFromSort() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().filters().sortButton().click();
        basePageSteps.onListingPage().popup().radioButtonWithLabel("Сначала дешевле").click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при смене фильтра состояния")
    public void shouldSeeUpdatedQueryIdFromFilterCondition() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel("Новый").click();
        basePageSteps.onListingPage().allFiltersPopup().show();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при смене фильтра цены")
    public void shouldSeeUpdatedQueryIdFromFilterPrice() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys("100");
        basePageSteps.onListingPage().allFiltersPopup().show();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

}
