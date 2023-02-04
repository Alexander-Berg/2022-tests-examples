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
import static ru.yandex.general.consts.Pages.CHELYABINSK;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.mobile.element.FiltersPopup.ADDRESS_AND_RADIUS;
import static ru.yandex.general.mobile.element.FiltersPopup.DISTRICT_NAME;
import static ru.yandex.general.mobile.element.FiltersPopup.METRO;
import static ru.yandex.general.mobile.element.FiltersPopup.REGION;
import static ru.yandex.general.mobile.element.FiltersPopup.STATION_OR_LINE;
import static ru.yandex.general.mobile.page.ListingPage.DONE;
import static ru.yandex.general.mobile.page.ListingPage.SHOW_BUTTON;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SEARCH)
@DisplayName("Отправка «search», обновление queryId при изменении гео")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class SearchQueryIdGeoChangeTest {

    private static final String ADDRESS = "Ленинградский проспект, 80к17";
    private static final String ADDRESS_2 = "Сущёвская улица, 21с5";
    private static final String TRAKTOROZAVODSKIY = "Тракторозаводский район";
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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(REGION).click();
        basePageSteps.onListingPage().popup(REGION).input().sendKeys("Санкт-Петербург");
        basePageSteps.onListingPage().popup(REGION).spanLink("Санкт-Петербург").click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();

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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(ADDRESS_AND_RADIUS).click();
        basePageSteps.onListingPage().addressSuggestScreen().findAddressInput().sendKeys(ADDRESS);
        basePageSteps.onListingPage().addressSuggestScreen().spanLink(ADDRESS).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().wrapper(ADDRESS_AND_RADIUS).button(DONE).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();

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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();
        basePageSteps.onListingPage().popup().input(STATION_OR_LINE).sendKeys(PAVELECKAYA);
        basePageSteps.onListingPage().popup().spanLink(PAVELECKAYA).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при выборе района на главной")
    public void shouldSeeUpdatedQueryIdChooseDistrictHomepage() {
        urlSteps.testing().path(CHELYABINSK).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        eventSteps.clearHar();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).click();
        basePageSteps.onListingPage().popup().input(DISTRICT_NAME).sendKeys(TRAKTOROZAVODSKIY);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().spanLink(TRAKTOROZAVODSKIY).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при смене адреса на листинге категории")
    public void shouldSeeUpdatedQueryIdChangeAddressListing() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        eventSteps.clearHar();

        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(ADDRESS_AND_RADIUS).click();
        basePageSteps.onListingPage().addressSuggestScreen().findAddressInput().sendKeys(ADDRESS);
        basePageSteps.onListingPage().addressSuggestScreen().spanLink(ADDRESS).click();
        basePageSteps.onListingPage().wrapper(ADDRESS_AND_RADIUS).button(DONE).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();

        queryId = eventSteps.getQueryId();
        eventSteps.clearHar();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(ADDRESS_AND_RADIUS).click();
        basePageSteps.onListingPage().wrapper(ADDRESS_AND_RADIUS).input().clearInput().click();
        basePageSteps.onListingPage().addressSuggestScreen().findAddressInput().sendKeys(ADDRESS_2);
        basePageSteps.onListingPage().addressSuggestScreen().spanLink(ADDRESS_2).click();
        basePageSteps.onListingPage().wrapper(ADDRESS_AND_RADIUS).button(DONE).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();

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

        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();
        basePageSteps.onListingPage().popup().input(STATION_OR_LINE).sendKeys(PAVELECKAYA);
        basePageSteps.onListingPage().popup().spanLink(PAVELECKAYA).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId отличается при выборе района на листинге категории")
    public void shouldSeeUpdatedQueryIdChooseDistrictListing() {
        urlSteps.testing().path(CHELYABINSK).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();
        eventSteps.clearHar();

        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).click();
        basePageSteps.onListingPage().popup().input(DISTRICT_NAME).sendKeys(TRAKTOROZAVODSKIY);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().spanLink(TRAKTOROZAVODSKIY).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();

        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).queryIdMatcher(not(equalTo(queryId))).shouldExist();
    }

}
