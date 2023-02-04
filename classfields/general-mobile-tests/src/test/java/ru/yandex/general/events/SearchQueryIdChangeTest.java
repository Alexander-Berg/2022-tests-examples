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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
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
import static ru.yandex.general.mobile.element.Wrapper.FIND;
import static ru.yandex.general.page.ListingPage.PRICE;
import static ru.yandex.general.page.ListingPage.PRICE_FROM;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SEARCH)
@DisplayName("Отправка «search», обновление queryId")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
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

        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(SEARCH_TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();

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

        basePageSteps.onListingPage().homeCategory("Электроника").click();

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

        basePageSteps.scrollToBottom();
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

        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(SEARCH_TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();

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

        basePageSteps.onListingPage().header().oLogo().click();

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
        basePageSteps.resize(375, 1500);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();

        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().spanLink("Сначала дешевле").click();
        basePageSteps.onListingPage().filters().showOffers().click();

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

        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel("Новый").click();
        basePageSteps.onListingPage().filters().showOffers().click();

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

        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_FROM).sendKeys("100");
        basePageSteps.onListingPage().filters().showOffers().click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

}
