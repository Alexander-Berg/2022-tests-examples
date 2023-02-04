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
import ru.yandex.general.mock.MockFavoritesSnippet;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.events.ChatInit.chatInit;
import static ru.yandex.general.beans.events.Context.context;
import static ru.yandex.general.beans.events.Event.event;
import static ru.yandex.general.beans.events.EventInfo.eventInfo;
import static ru.yandex.general.beans.events.Offer.offer;
import static ru.yandex.general.beans.events.Price.price;
import static ru.yandex.general.beans.events.TrafficSource.trafficSource;
import static ru.yandex.general.consts.Events.BLOCK_LISTING;
import static ru.yandex.general.consts.Events.CHAT_INIT;
import static ru.yandex.general.consts.Events.PAGE_FAVORITES;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_CHAT_INIT;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockChat.chatTemplate;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockFavoritesSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_CHAT_INIT)
@DisplayName("Отправка «chatInit» со страницы избранного")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ChatInitFavoritesSendEventTest {

    private static final long PRICE = 32000;
    private static final String ID = "123456";
    private static final String OFFER_VERSION = "13";
    private static final String CATEGORY_ID = "koshki_oyCgxy";
    private static final int PHOTO_COUNT = 7;
    private static final String REGION_ID = "213";
    private static final String ANY = "Any";
    private static final String CHAT = "Chat";
    private static final String FIRST_PHOTO_URL = "https://avatars.mdst.yandex.net/get-o-yandex/65675/9a8cfa211a2c56004de15adea346688f/520x692";
    private static final String[] JSONPATHS_TO_IGNORE = {"eventTime", "queryId"};

    private Event event;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private EventSteps eventSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public MockFavoritesSnippet snippet;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Отправка «chatInit» со страницы избранного по нажатию на иконку сообщений",
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(ANY)
                },
                {"Отправка «chatInit» со страницы избранного по нажатию на кнопку «Написать»",
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(CHAT)
                }
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();

        urlSteps.testing().path(MY).path(FAVORITES);

        mockRule.graphqlStub(mockResponse()
                .setFavorites(favoritesResponse().offers(asList(snippet
                        .setId(ID)
                        .setPrice(PRICE)
                        .setCategoryId(CATEGORY_ID)
                        .addPhoto(PHOTO_COUNT)
                        .setOfferVersion(OFFER_VERSION))).build())
                .setChat(chatTemplate().setIsNew(true).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        event = event().setEventInfo(eventInfo().setChatInit(chatInit().setOffer(
                offer().setPrice(price(PRICE))
                        .setOfferId(ID)
                        .setCategoryId(CATEGORY_ID)
                        .setOfferVersion(OFFER_VERSION)
                        .setPhotoCount(PHOTO_COUNT)
                        .setFirstPhotoUrl(FIRST_PHOTO_URL))))
                .setContext(context().setBlock(BLOCK_LISTING)
                        .setPage(PAGE_FAVORITES)
                        .setReferer(urlSteps.toString()))
                .setPortalRegionId(REGION_ID)
                .setTrafficSource(trafficSource());

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);

        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка «chatInit» со страницы избранного, для нового чата")
    public void shouldSeeChatInitByChatButton() {
        basePageSteps.onFavoritesPage().firstFavCard().chatButton().hover().click();

        eventSteps.withEventType(CHAT_INIT).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

}
