package ru.yandex.general.events;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
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
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SEARCH)
@DisplayName("Отправка «search», обновление queryId при изменении гео")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class SearchQueryIdGeoChangeTest {

    private static final String ADDRESS = "Ленинградский проспект, 80к17";
    private static final String ADDRESS_2 = "Сущёвская улица, 21с5";
    private static final String SILINO = "Силино";
    private static final String PAVELECKAYA = "Павелецкая";

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
    @DisplayName("QueryId отличается при смене региона на главной")
    public void shouldSeeUpdatedQueryIdChangeRegionHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();
        basePageSteps.onListingPage().region().click();
        basePageSteps.onListingPage().searchBar().suggest().spanLink("Санкт-Петербург").click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при выборе адреса на главной")
    public void shouldSeeUpdatedQueryIdChooseAddressHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(ADDRESS);
        basePageSteps.onListingPage().searchBar().suggestItem(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при выборе метро на главной")
    public void shouldSeeUpdatedQueryIdChooseSubwayHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station(PAVELECKAYA).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при выборе района на главной")
    public void shouldSeeUpdatedQueryIdChooseDistrictHomepage() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(SILINO).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при смене адреса на листинге категории")
    public void shouldSeeUpdatedQueryIdChangeAddressListing() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        eventSteps.clearHar();

        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(ADDRESS);
        basePageSteps.onListingPage().searchBar().suggestItem(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().searchBar().suggest().waitUntil(not(isDisplayed()));
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();
        eventSteps.clearHar();

        basePageSteps.onListingPage().searchBar().button(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(ADDRESS_2);
        basePageSteps.onListingPage().searchBar().suggestItem(ADDRESS_2).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при выборе метро на листинге категории")
    public void shouldSeeUpdatedQueryIdChooseSubwayListing() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();
        eventSteps.clearHar();

        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station(PAVELECKAYA).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при выборе района на листинге категории")
    public void shouldSeeUpdatedQueryIdChooseDistrictListing() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(SILINO).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

}
