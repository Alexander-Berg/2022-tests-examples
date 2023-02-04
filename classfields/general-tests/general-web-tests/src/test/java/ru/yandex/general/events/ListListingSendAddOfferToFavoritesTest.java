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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.events.AddOfferToFavorites.addOfferToFavorites;
import static ru.yandex.general.beans.events.Context.context;
import static ru.yandex.general.beans.events.Event.event;
import static ru.yandex.general.beans.events.EventInfo.eventInfo;
import static ru.yandex.general.beans.events.Offer.offer;
import static ru.yandex.general.beans.events.Price.price;
import static ru.yandex.general.beans.events.TrafficSource.trafficSource;
import static ru.yandex.general.consts.Events.ADD_OFFER_TO_FAVORITES;
import static ru.yandex.general.consts.Events.BLOCK_LISTING;
import static ru.yandex.general.consts.Events.PAGE_LISTING;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_ADD_OFFER_TO_FAVORITES;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.general.step.BasePageSteps.LIST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_ADD_OFFER_TO_FAVORITES)
@DisplayName("Отправка «addOfferToFavorites» с листинга категории, листинг списком")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class ListListingSendAddOfferToFavoritesTest {

    private static final String REGION_ID = "2";
    private static final long PRICE = 32000;
    private static final String ID = "123456";
    private static final String OFFER_VERSION = "13";
    private static final String CATEGORY_ID = "koshki_oyCgxy";
    private static final int PHOTO_COUNT = 7;
    private static final String FIRST_PHOTO_URL = "https://avatars.mdst.yandex.net/get-o-yandex/65675/9a8cfa211a2c56004de15adea346688f/520x692";
    private static final String COLOR = "#f9f1f1";
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

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(SANKT_PETERBURG).path(ELEKTRONIKA).open();

        mockRule.graphqlStub(mockResponse().setSearch(listingCategoryResponse().offers(asList(
                mockSnippet(BASIC_SNIPPET).getMockSnippet()
                        .setId(ID)
                        .setOfferVersion(OFFER_VERSION)
                        .setCategoryId(CATEGORY_ID)
                        .setPrice(PRICE)
                        .addPhoto(PHOTO_COUNT)
                        .setMainColor(COLOR))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        event = event().setContext(context().setBlock(BLOCK_LISTING).setPage(PAGE_LISTING)
                .setReferer(urlSteps.toString()))
                .setPortalRegionId(REGION_ID)
                .setTrafficSource(trafficSource())
                .setEventInfo(eventInfo().setAddOfferToFavorites(addOfferToFavorites().setOffer(offer()
                        .setPrice(price(PRICE))
                        .setOfferId(ID)
                        .setCategoryId(CATEGORY_ID)
                        .setOfferVersion(OFFER_VERSION)
                        .setPhotoCount(PHOTO_COUNT)
                        .setFirstPhotoUrl(FIRST_PHOTO_URL))));

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);

        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «addOfferToFavorites» с листинга категории, листинг списком")
    public void shouldSeeAddOfferToFavoritesSendEventFromListings() {
        basePageSteps.onListingPage().snippetFirst().waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().click();

        eventSteps.withEventType(ADD_OFFER_TO_FAVORITES).singleEventWithParams(event)
                .withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

}
