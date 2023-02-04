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
import ru.yandex.general.beans.events.Offer;
import ru.yandex.general.mock.MockListingSnippet;
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
import static ru.yandex.general.beans.events.Offer.offer;
import static ru.yandex.general.beans.events.Price.price;
import static ru.yandex.general.beans.events.SnippetClick.snippetClick;
import static ru.yandex.general.beans.events.SnippetShow.snippetShow;
import static ru.yandex.general.beans.events.TrafficSource.trafficSource;
import static ru.yandex.general.consts.Events.BLOCK_LISTING;
import static ru.yandex.general.consts.Events.PAGE_LISTING;
import static ru.yandex.general.consts.Events.SNIPPET_CLICK;
import static ru.yandex.general.consts.Events.SNIPPET_SHOW;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SNIPPET_CLICK;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SNIPPET_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.REZUME_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(EVENTS_FEATURE)
@DisplayName("Отправка «snippetClick», «snippetShow» с разными типами price")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingDifferentPriceTypesSendEventTest {

    private static final String REGION_ID = "2";
    private static final long PRICE = 32000;
    private static final String SALLARY_PRICE = "51000";
    private static final String ID = "123456";
    private static final String OFFER_VERSION = "13";
    private static final String CATEGORY_ID = "koshki_oyCgxy";
    private static final int PHOTO_COUNT = 7;
    private static final String FIRST_PHOTO_URL = "https://avatars.mdst.yandex.net/get-o-yandex/65675/9a8cfa211a2c56004de15adea346688f/520x692";
    private static final String COLOR = "#f9f1f1";
    private static final int FIRST = 1;
    private static final String RATIO = "Ratio1x1";
    private static final String[] JSONPATHS_TO_IGNORE = {"eventTime", "queryId"};

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
    public String title;

    @Parameterized.Parameter(1)
    public MockListingSnippet snippet;

    @Parameterized.Parameter(2)
    public Offer eventOffer;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Событие с листинга. цена указана",
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPrice(PRICE),
                        offer().setPrice(price(PRICE))
                },
                {"Событие с листинга. цена - «Даром»",
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setFreePrice(),
                        offer().setPrice(price())
                },
                {"Событие с листинга. цена не указана",
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setUnsetPrice(),
                        offer().setPrice(price())
                },
                {"Событие с листинга. зарплата",
                        mockSnippet(REZUME_SNIPPET).getMockSnippet().setSallaryPrice(SALLARY_PRICE),
                        offer().setPrice(price(Long.parseLong(SALLARY_PRICE)))
                }
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(SANKT_PETERBURG).path(ELEKTRONIKA);

        mockRule.graphqlStub(mockResponse()
                .setSearch(listingCategoryResponse().offers(
                        asList(snippet.setId(ID)
                                .setOfferVersion(OFFER_VERSION)
                                .setCategoryId(CATEGORY_ID)
                                .addPhoto(PHOTO_COUNT)
                                .setMainColor(COLOR))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        event = event().setContext(context().setBlock(BLOCK_LISTING)
                .setPage(PAGE_LISTING)
                .setReferer(urlSteps.toString()))
                .setPortalRegionId(REGION_ID)
                .setTrafficSource(trafficSource());
        eventOffer.setOfferId(ID)
                .setCategoryId(CATEGORY_ID)
                .setOfferVersion(OFFER_VERSION)
                .setPhotoCount(PHOTO_COUNT)
                .setFirstPhotoUrl(FIRST_PHOTO_URL);

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);

        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        eventSteps.clearHar();
        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(EVENT_SNIPPET_CLICK)
    @DisplayName("Отправка «snippetClick» с листинга, с разными типами price")
    public void shouldSeeSnippetClickPriceTypesSendEvent() {
        basePageSteps.onListingPage().snippetFirst().waitUntil(isDisplayed()).hover().click();

        event.setEventInfo(eventInfo().setSnippetClick(snippetClick().setOfferSnippet(
                eventOffer.setColorHex(COLOR).setSnippetRatio(RATIO)).setPage(FIRST).setIndex(FIRST)));

        eventSteps.withEventType(SNIPPET_CLICK).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(EVENT_SNIPPET_SHOW)
    @DisplayName("Отправка «snippetShow» с листинга, с разными типами price")
    public void shouldSeeSnippetShowPriceTypesSendEvent() {
        basePageSteps.scrollingToElement(basePageSteps.onListingPage().snippetFirst());

        event.setEventInfo(eventInfo().setSnippetShow(snippetShow().setOfferSnippet(
                eventOffer.setColorHex(COLOR).setSnippetRatio(RATIO)).setPage(FIRST).setIndex(FIRST)));

        eventSteps.withEventType(SNIPPET_SHOW).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

}
