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
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.events.CardView.cardView;
import static ru.yandex.general.beans.events.Context.context;
import static ru.yandex.general.beans.events.Event.event;
import static ru.yandex.general.beans.events.EventInfo.eventInfo;
import static ru.yandex.general.beans.events.Offer.offer;
import static ru.yandex.general.beans.events.PhoneCall.phoneCall;
import static ru.yandex.general.beans.events.Price.price;
import static ru.yandex.general.beans.events.TrafficSource.trafficSource;
import static ru.yandex.general.consts.Events.BLOCK_CARD;
import static ru.yandex.general.consts.Events.CARD_VIEW;
import static ru.yandex.general.consts.Events.PAGE_CARD;
import static ru.yandex.general.consts.Events.PHONE_CALL;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_CARD_VIEW;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_PHONE_CALL;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.PHOTO_1;
import static ru.yandex.general.mock.MockCard.PHOTO_2;
import static ru.yandex.general.mock.MockCard.PHOTO_3;
import static ru.yandex.general.mock.MockCard.REZUME_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockChat.chatTemplate;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;

@Epic(EVENTS_FEATURE)
@DisplayName("Отправка «phoneCall», «cardView», карточка с разными типами price")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardDifferentPriceTypesSendEventsTest {

    private static final String REGION_ID = "2";
    private static final long PRICE = 32000;
    private static final String ID = "123456";
    private static final String OFFER_VERSION = "13";
    private static final String CATEGORY_ID = "koshki_oyCgxy";
    private static final int PHOTO_COUNT = 3;
    private static final String SALLARY_PRICE = "51000";
    private static final String FIRST_PHOTO_URL = "https://avatars.mdst.yandex.net/get-o-yandex/65675/af6807fe8f1796887c7e6907389a38f9/1556x1172";
    private static final String[] JSONPATHS_TO_IGNORE = {"eventTime", "queryId"};
    private static final String ANY = "Any";

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
    public MockCard card;

    @Parameterized.Parameter(2)
    public Offer eventOffer;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Событие с карточки оффера. цена указана",
                        mockCard(BASIC_CARD).setPreferContactWay(ANY).setPrice(PRICE),
                        offer().setPrice(price(PRICE))
                },
                {"Событие с карточки оффера. цена - «Даром»",
                        mockCard(BASIC_CARD).setPreferContactWay(ANY).setFreePrice(),
                        offer().setPrice(price())
                },
                {"Событие с карточки оффера. цена не указана",
                        mockCard(BASIC_CARD).setPreferContactWay(ANY).setUnsetPrice(),
                        offer().setPrice(price())
                },
                {"Событие с карточки оффера. зарплата",
                        mockCard(REZUME_CARD).setPreferContactWay(ANY).setSallaryPrice(SALLARY_PRICE),
                        offer().setPrice(price(Long.parseLong(SALLARY_PRICE)))
                }
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CARD).path(ID).path(SLASH);

        mockRule.graphqlStub(mockResponse()
                .setCard(card.setId(ID)
                        .setCategoryId(CATEGORY_ID)
                        .addPhoto(PHOTO_1, PHOTO_2, PHOTO_3)
                        .setOfferVersion(OFFER_VERSION).build())
                .setChat(chatTemplate().setIsNew(true).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);

        event = event().setContext(context().setBlock(BLOCK_CARD)
                .setPage(PAGE_CARD)
                .setReferer(urlSteps.toString()))
                .setPortalRegionId(REGION_ID)
                .setTrafficSource(trafficSource());
        eventOffer.setOfferId(ID)
                .setCategoryId(CATEGORY_ID)
                .setOfferVersion(OFFER_VERSION)
                .setPhotoCount(PHOTO_COUNT)
                .setFirstPhotoUrl(FIRST_PHOTO_URL);

        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(EVENT_PHONE_CALL)
    @DisplayName("Отправка «phoneCall» с карточки оффера, с разными типами price")
    public void shouldSeePhoneCallSendEventOnCardPriceTypes() {
        basePageSteps.onOfferCardPage().sidebar().showPhone().click();

        event.setEventInfo(eventInfo().setPhoneCall(phoneCall().setOffer(eventOffer)));

        eventSteps.withEventType(PHONE_CALL).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(EVENT_CARD_VIEW)
    @DisplayName("Отправка «cardView» с карточки оффера, с разными типами price")
    public void shouldSeeCardViewSendEventOnCardPriceTypes() {
        event.setEventInfo(eventInfo().setCardView(cardView().setOffer(eventOffer)));

        eventSteps.withEventType(CARD_VIEW).singleEventWithParams(event).withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }

}
