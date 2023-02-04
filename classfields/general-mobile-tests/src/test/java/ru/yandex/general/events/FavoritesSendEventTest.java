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
import ru.yandex.general.beans.events.Event;
import ru.yandex.general.beans.events.Offer;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.events.Context.context;
import static ru.yandex.general.beans.events.Event.event;
import static ru.yandex.general.beans.events.EventInfo.eventInfo;
import static ru.yandex.general.beans.events.Offer.offer;
import static ru.yandex.general.beans.events.PhoneCall.phoneCall;
import static ru.yandex.general.beans.events.Price.price;
import static ru.yandex.general.beans.events.SnippetClick.snippetClick;
import static ru.yandex.general.beans.events.SnippetShow.snippetShow;
import static ru.yandex.general.beans.events.TrafficSource.trafficSource;
import static ru.yandex.general.consts.Events.BLOCK_LISTING;
import static ru.yandex.general.consts.Events.PAGE_FAVORITES;
import static ru.yandex.general.consts.Events.PHONE_CALL;
import static ru.yandex.general.consts.Events.SNIPPET_CLICK;
import static ru.yandex.general.consts.Events.SNIPPET_SHOW;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_PHONE_CALL;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SNIPPET_CLICK;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SNIPPET_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockFavoritesSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(EVENTS_FEATURE)
@DisplayName("Отправка событий «snippetClick», «snippetShow», «phoneCall» со страницы избранных офферов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class FavoritesSendEventTest {

    private static final String REGION_ID = "213";
    private static final String PHONE = "+79118887766";
    private static final long PRICE = 32000;
    private static final String ID = "123456";
    private static final String OFFER_VERSION = "13";
    private static final String CATEGORY_ID = "koshki_oyCgxy";
    private static final int PHOTO_COUNT = 7;
    private static final String COLOR = "#f9f1f1";
    private static final int FIRST = 1;
    private static final String RATIO = "Ratio1x1";
    private static final String FIRST_PHOTO_URL = "https://avatars.mdst.yandex.net/get-o-yandex/65675/9a8cfa211a2c56004de15adea346688f/520x692";
    private static final String[] JSONPATHS_TO_IGNORE = {"eventTime", "queryId"};

    private Event event;
    private Offer eventOffer;

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
        urlSteps.testing().path(MY).path(FAVORITES);

        mockRule.graphqlStub(mockResponse().setFavorites(
                favoritesResponse().offers(asList(mockSnippet(BASIC_SNIPPET).getMockSnippet()
                        .setId(ID)
                        .setPrice(PRICE)
                        .setCategoryId(CATEGORY_ID)
                        .addPhoto(PHOTO_COUNT)
                        .setMainColor(COLOR)
                        .setOfferVersion(OFFER_VERSION))).build())
                .setOfferPhone(PHONE)
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();


        event = event().setContext(context().setBlock(BLOCK_LISTING)
                .setPage(PAGE_FAVORITES)
                .setReferer(urlSteps.toString()))
                .setPortalRegionId(REGION_ID)
                .setTrafficSource(trafficSource());
        eventOffer = offer().setOfferId(ID)
                .setPrice(price(PRICE))
                .setCategoryId(CATEGORY_ID)
                .setOfferVersion(OFFER_VERSION)
                .setPhotoCount(PHOTO_COUNT)
                .setFirstPhotoUrl(FIRST_PHOTO_URL);

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);

        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        eventSteps.clearHar();
        urlSteps.open();
    }

    @Test
    @Ignore("Чиним тут CLASSFRONT-1102")
    @Owner(ALEKS_IVANOV)
    @Feature(EVENT_SNIPPET_CLICK)
    @DisplayName("Отправка «snippetClick» со страницы избранных офферов")
    public void shouldSeeSnippetClickSendEventFromFavorites() {
        basePageSteps.onFavoritesPage().firstFavCard().click();

        event.setEventInfo(eventInfo().setSnippetClick(snippetClick().setOfferSnippet(
                eventOffer.setColorHex(COLOR).setSnippetRatio(RATIO)).setPage(FIRST).setIndex(FIRST)));

        eventSteps.withEventType(SNIPPET_CLICK).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(EVENT_SNIPPET_SHOW)
    @DisplayName("Отправка «snippetShow» со страницы избранных офферов")
    public void shouldSeeSnippetShowSendEventFromFavorites() {
        basePageSteps.scrollingToElement(basePageSteps.onFavoritesPage().firstFavCard());

        event.setEventInfo(eventInfo().setSnippetShow(snippetShow().setOfferSnippet(
                eventOffer.setColorHex(COLOR).setSnippetRatio(RATIO)).setPage(FIRST).setIndex(FIRST)));

        eventSteps.withEventType(SNIPPET_SHOW).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(EVENT_PHONE_CALL)
    @DisplayName("Отправка «phoneCall» со страницы избранных офферов")
    public void shouldSeePhoneCallSendEventFromFavorites() {
        basePageSteps.onFavoritesPage().firstFavCard().phoneShow().click();

        event.setEventInfo(eventInfo().setPhoneCall(phoneCall().setOffer(eventOffer)));

        eventSteps.withEventType(PHONE_CALL).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

}
