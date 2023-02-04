package ru.yandex.realty.eventslog.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.step.GoalsSteps.EVENT_GATE_STAT_CARD;

@DisplayName("Событие «card_show»")
@Feature(RealtyFeatures.EVENTS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class OpenOfferCardEventTest {

    private static final String PATH_TO_OPEN_OFFER_CARD_EVENT_JSON = "events/openOfferCardEvent.json";
    private static final String[] JSONPATHS_TO_IGNORE = {"params[0][1].query_id"};
    private MockOffer offer;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private ProxySteps proxy;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Открытие карточки оффера. «card_show»")
    public void shouldSeeOpenOfferCardEvent() {
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
        proxy.clearHar();
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        goalsSteps.urlMatcher(containsString(EVENT_GATE_STAT_CARD)).withEventParams(PATH_TO_OPEN_OFFER_CARD_EVENT_JSON)
                .withIgnoringPaths(JSONPATHS_TO_IGNORE).shouldExist();
    }
}