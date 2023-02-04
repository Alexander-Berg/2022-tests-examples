package ru.yandex.realty.eventslog.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.GoalsSteps.EVENT_GATE_STAT_OFFER;

@DisplayName("Событие «offer_show»")
@Feature(RealtyFeatures.EVENTS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class ListingScrollEventTest {

    private static final String PATH_TO_SCROLLING_OFFER_SHOW_EVENT = "events/scrollingOfferShowEvent.json";
    private static final String[] JSONPATHS_TO_IGNORE = {"params[1][1].newLog.requestContext.queryInfo.queryId",
            "params[0][1].newLog.requestContext.queryInfo.queryId", "params[1][1].query_id", "params[0][1].query_id"};
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

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
    }

    @Test
    @DisplayName("Открытие листинга скроллим и видим событие «offer_show»")
    @Owner(KANTEMIROV)
    public void shouldSeeScrollingOfferShowEvent() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer))
                .build()).createWithDefaults();
        proxy.clearHar();
        basePageSteps.resize(1600, 300);
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.scrolling(1000, 100);
        goalsSteps.urlMatcher(containsString(EVENT_GATE_STAT_OFFER))
                .withEventParams(PATH_TO_SCROLLING_OFFER_SHOW_EVENT)
                .withIgnoringPaths(JSONPATHS_TO_IGNORE)
                .shouldExist();
    }
}
