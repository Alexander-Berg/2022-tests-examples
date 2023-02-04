package ru.yandex.general.events;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.beans.events.Event;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.events.Context.context;
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
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_ASC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_DESC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PUBLISH_DATE_DESC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SEARCH)
@DisplayName("Отправка событий «search» с выбранным типом сортировки")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SearchSendEventSortingTypesTest {

    private static final String REGION_ID = "213";
    private static final String[] JSONPATHS_TO_IGNORE = {"eventTime", "queryId", "eventInfo.search.offerCountByCategory", "eventInfo.search.searchRequestId"};

    private Event event;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private EventSteps eventSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String sortName;

    @Parameterized.Parameter(1)
    public String sortParamValue;

    @Parameterized.Parameters(name = "Сортировка «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"По актуальности", SORT_BY_RELEVANCE_VALUE},
                {"Сначала свежие", SORT_BY_PUBLISH_DATE_DESC_VALUE},
                {"Сначала дешевле", SORT_BY_PRICE_ASC_VALUE},
                {"Сначала дороже", SORT_BY_PRICE_DESC_VALUE}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(SORTING_PARAM, sortParamValue);

        event = event().setEventInfo(eventInfo().setSearch(search()
                .setSorting(sortParamValue)
                .setSearchUrl(urlSteps.toString())
                .setSearchArea(searchArea().setToponyms(toponyms().setRegion(REGION_ID)))
                .setSearchText("")
                .setPage(page().setPage(DEFAULT_PAGE_NUMBER).setLimit(DEFAULT_PAGE_LIMIT))))
                .setPortalRegionId(REGION_ID)
                .setTrafficSource(trafficSource());

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);

        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        eventSteps.clearHar();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «search» с выбранным типом сортировки, context.block = «BlockListing»")
    public void shouldSeeSearchEventWithSortType() {
        urlSteps.open();

        event.setContext(context().setBlock(BLOCK_LISTING)
                .setPage(PAGE_LISTING)
                .setReferer(urlSteps.toString()));

        eventSteps.withEventType(SEARCH).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

}
