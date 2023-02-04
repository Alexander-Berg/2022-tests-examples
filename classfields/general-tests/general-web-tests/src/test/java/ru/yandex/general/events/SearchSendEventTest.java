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
import ru.yandex.general.beans.events.Event;
import ru.yandex.general.beans.events.Search;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.events.Context.context;
import static ru.yandex.general.beans.events.Coordinates.coordinates;
import static ru.yandex.general.beans.events.Event.event;
import static ru.yandex.general.beans.events.EventInfo.eventInfo;
import static ru.yandex.general.beans.events.Page.page;
import static ru.yandex.general.beans.events.Search.search;
import static ru.yandex.general.beans.events.SearchArea.searchArea;
import static ru.yandex.general.beans.events.Toponyms.toponyms;
import static ru.yandex.general.beans.events.TrafficSource.trafficSource;
import static ru.yandex.general.consts.Events.BLOCK_LISTING;
import static ru.yandex.general.consts.Events.DEFAULT_PAGE_LIMIT;
import static ru.yandex.general.consts.Events.DEFAULT_PAGE_NUMBER;
import static ru.yandex.general.consts.Events.PAGE_LISTING;
import static ru.yandex.general.consts.Events.SEARCH;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SEARCH;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOBILNIE_TELEFONI;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.STATE_USED;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PUBLISH_DATE_DESC_VALUE;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SEARCH)
@DisplayName("Отправка событий «search»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class SearchSendEventTest {

    private static final String TEXT = "телевизор";
    private static final String REGION_ID = "213";
    private static final String DEFAULT_SORT = "ByRelevance";
    private static final String METRO_1 = "20384";
    private static final String METRO_2 = "20475";
    private static final String DISTRICT_1 = "117067";
    private static final String DISTRICT_2 = "120540";
    private static final String LATITUDE = "55.751666";
    private static final String LONGITUDE = "37.63349";
    private static final String RADIUS = "10000";
    private static final String PRICE_MIN = "5000";
    private static final String PRICE_MAX = "25000";
    private static final String[] JSONPATHS_TO_IGNORE = {"eventTime", "queryId", "eventInfo.search.offerCountByCategory", "eventInfo.search.searchRequestId"};

    private Event event;
    private Search search;

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
        search = search()
                .setSorting(DEFAULT_SORT)
                .setSearchArea(searchArea().setToponyms(toponyms().setRegion(REGION_ID)))
                .setSearchText("")
                .setPage(page().setPage(DEFAULT_PAGE_NUMBER).setLimit(DEFAULT_PAGE_LIMIT));
        event = event()
                .setContext(context().setBlock(BLOCK_LISTING).setPage(PAGE_LISTING))
                .setPortalRegionId(REGION_ID)
                .setTrafficSource(trafficSource());

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        eventSteps.clearHar();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «search», поисковый текст")
    public void shouldSeeSearchEventText() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();

        event.setEventInfo(eventInfo().setSearch(search.setSearchText(TEXT)
                .setSearchUrl(urlSteps.toString())))
                .getContext().setReferer(urlSteps.toString());

        eventSteps.withEventType(SEARCH).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «search», поисковый текст + метро")
    public void shouldSeeSearchEventTextMetro() {
        urlSteps.testing().path(MOSKVA).queryParam(METRO_ID_PARAM, METRO_1).queryParam(METRO_ID_PARAM, METRO_2)
                .queryParam(TEXT_PARAM, TEXT).open();

        event.setEventInfo(eventInfo().setSearch(
                search.setSearchText(TEXT)
                        .setSearchUrl(urlSteps.toString())
                        .setSearchArea(searchArea()
                                .setToponyms(toponyms().setMetro(asList(METRO_1, METRO_2)).setRegion(REGION_ID)))))
                .getContext().setReferer(urlSteps.toString());

        eventSteps.withEventType(SEARCH).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «search», поисковый текст + районы")
    public void shouldSeeSearchEventTextDistrict() {
        urlSteps.testing().path(MOSKVA).queryParam(DISTRICT_ID_PARAM, DISTRICT_1)
                .queryParam(DISTRICT_ID_PARAM, DISTRICT_2).queryParam(TEXT_PARAM, TEXT).open();

        event.setEventInfo(eventInfo().setSearch(
                search.setSearchText(TEXT)
                        .setSearchUrl(urlSteps.toString())
                        .setSearchArea(searchArea()
                                .setToponyms(toponyms().setDistricts(asList(DISTRICT_1, DISTRICT_2))
                                        .setRegion(REGION_ID)))))
                .getContext().setReferer(urlSteps.toString());

        eventSteps.withEventType(SEARCH).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «search», поисковый текст + координаты")
    public void shouldSeeSearchEventTextCoordinates() {
        urlSteps.testing().path(MOSKVA).queryParam(LATITUDE_PARAM, LATITUDE).queryParam(LONGITUDE_PARAM, LONGITUDE)
                .queryParam(GEO_RADIUS_PARAM, RADIUS).queryParam(TEXT_PARAM, TEXT).open();

        event.setEventInfo(eventInfo().setSearch(
                search.setSearchText(TEXT)
                        .setSearchUrl(urlSteps.toString())
                        .setSearchArea(searchArea()
                                .setCoordinates(coordinates()
                                        .setLatitude(Double.parseDouble(LATITUDE))
                                        .setLongitude(Double.parseDouble(LONGITUDE))
                                        .setRadiusMeters(Long.parseLong(RADIUS))))))
                .getContext().setReferer(urlSteps.toString());

        eventSteps.withEventType(SEARCH).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «search» категория листинга")
    public void shouldSeeSearchEventParentCategory() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();

        event.setEventInfo(eventInfo().setSearch(search
                .setSearchUrl(urlSteps.toString())))
                .getContext().setReferer(urlSteps.toString());

        eventSteps.withEventType(SEARCH).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «search», скоуп query параметров")
    public void shouldSeeSearchEventManyParameters() {
        urlSteps.testing().path(MOSKVA).path(MOBILNIE_TELEFONI).path(STATE_USED)
                .queryParam(SORTING_PARAM, SORT_BY_PUBLISH_DATE_DESC_VALUE)
                .queryParam(METRO_ID_PARAM, METRO_1)
                .queryParam(PRICE_MIN_URL_PARAM, PRICE_MIN)
                .queryParam(PRICE_MAX_URL_PARAM, PRICE_MAX)
                .queryParam("offer.attributes.proizvoditel-mobilnogo-telefona_454ghb", "apple")
                .queryParam("offer.attributes.operacionnaya-sistema_4925670_RAoXLJ", "ios").open();

        event.setEventInfo(eventInfo().setSearch(search
                .setSearchUrl(urlSteps.toString())
                .setSorting(SORT_BY_PUBLISH_DATE_DESC_VALUE)
                .setSearchArea(searchArea().setToponyms(toponyms()
                        .setMetro(asList(METRO_1)).setRegion(REGION_ID)))))
                .getContext().setReferer(urlSteps.toString());

        eventSteps.withEventType(SEARCH).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

}
