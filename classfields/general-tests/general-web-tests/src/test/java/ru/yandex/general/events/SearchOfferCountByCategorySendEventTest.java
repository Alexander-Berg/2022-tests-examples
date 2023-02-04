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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.events.Context.context;
import static ru.yandex.general.beans.events.Event.event;
import static ru.yandex.general.beans.events.EventInfo.eventInfo;
import static ru.yandex.general.beans.events.OfferCountByCategory.offerCountByCategory;
import static ru.yandex.general.beans.events.Page.page;
import static ru.yandex.general.beans.events.Search.search;
import static ru.yandex.general.beans.events.SearchArea.searchArea;
import static ru.yandex.general.beans.events.Toponyms.toponyms;
import static ru.yandex.general.beans.events.TrafficSource.trafficSource;
import static ru.yandex.general.consts.Events.BLOCK_LISTING;
import static ru.yandex.general.consts.Events.BLOCK_QUERY_EXPANSION;
import static ru.yandex.general.consts.Events.DEFAULT_PAGE_LIMIT;
import static ru.yandex.general.consts.Events.DEFAULT_PAGE_NUMBER;
import static ru.yandex.general.consts.Events.PAGE_LISTING;
import static ru.yandex.general.consts.Events.SEARCH;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SEARCH;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SEARCH)
@DisplayName("Отправка «search», проверка «offerCountByCategory»")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SearchOfferCountByCategorySendEventTest {

    private static final String DEFAULT_SORT = "ByRelevance";
    private static final String REGION_ID = "213";
    private static final String FIRST_CATEGORY_ID = "mobilnie-telefoni_OobNbL";
    private static final int FIRST_CATEGORY_OFFER_COUNT = 53;
    private static final String SECOND_CATEGORY_ID = "fotoapparati_FytMDN";
    private static final int SECOND_CATEGORY_OFFER_COUNT = 24;
    private static final String[] JSONPATHS_TO_IGNORE = {"eventTime", "queryId", "eventInfo.search.searchRequestId"};

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

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String contextBlock;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {BLOCK_LISTING},
                {BLOCK_QUERY_EXPANSION}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA);

        mockRule.graphqlStub(mockResponse().setSearch(listingCategoryResponse().offers(
                asList(mockSnippet(BASIC_SNIPPET).getMockSnippet())).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        event = event()
                .setEventInfo(eventInfo().setSearch(search()
                        .setOfferCountByCategory(asList(
                                offerCountByCategory().setCategoryId(FIRST_CATEGORY_ID)
                                        .setOfferCount(FIRST_CATEGORY_OFFER_COUNT),
                                offerCountByCategory().setCategoryId(SECOND_CATEGORY_ID)
                                        .setOfferCount(SECOND_CATEGORY_OFFER_COUNT)))
                        .setSorting(DEFAULT_SORT)
                        .setSearchArea(searchArea().setToponyms(toponyms().setRegion(REGION_ID)))
                        .setSearchText("")
                        .setSearchUrl(urlSteps.toString())
                        .setPage(page().setPage(DEFAULT_PAGE_NUMBER).setLimit(DEFAULT_PAGE_LIMIT))))
                .setContext(context().setBlock(contextBlock).setPage(PAGE_LISTING).setReferer(urlSteps.toString()))
                .setPortalRegionId(REGION_ID)
                .setTrafficSource(trafficSource());

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        eventSteps.clearHar();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «search», проверка «offerCountByCategory»")
    public void shouldSeeSearchWithOfferCountByCategory() {
        urlSteps.open();

        eventSteps.withEventType(SEARCH).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

}
