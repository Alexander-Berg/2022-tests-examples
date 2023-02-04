package ru.yandex.realty.goals.cardallshow;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.beans.Goal;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.GoalsConsts.Goal.CARD_ALL_SHOW;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.COMMERCIAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.GARAGE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.HOUSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.LOT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEW_FLAT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEW_SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OWN;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PER_MONTH;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PREMIUM;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PROMOTION;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RAISING;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.ROOMS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELECTED;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TURBO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.PROMOTED;
import static ru.yandex.realty.mock.MockOffer.RAISED;
import static ru.yandex.realty.mock.MockOffer.RENT_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.RENT_COMMERCIAL_WAREHOUSE;
import static ru.yandex.realty.mock.MockOffer.RENT_GARAGE;
import static ru.yandex.realty.mock.MockOffer.RENT_HOUSE;
import static ru.yandex.realty.mock.MockOffer.RENT_ROOM;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.SELL_COMMERCIAL;
import static ru.yandex.realty.mock.MockOffer.SELL_GARAGE;
import static ru.yandex.realty.mock.MockOffer.SELL_HOUSE;
import static ru.yandex.realty.mock.MockOffer.SELL_LOT;
import static ru.yandex.realty.mock.MockOffer.SELL_NEW_BUILDING_SECONDARY;
import static ru.yandex.realty.mock.MockOffer.SELL_NEW_SECONDARY;
import static ru.yandex.realty.mock.MockOffer.SELL_ROOM;
import static ru.yandex.realty.mock.MockOffer.TURBOSALE;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Цель «card.all.show»")
@Feature(RealtyFeatures.GOALS)
@RunWith(Parameterized.class)
@Issue("VERTISTEST-1208")
@GuiceModules(RealtyWebWithProxyModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OpenOfferCardGoalsTest {

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

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public MockOffer cardOfferResponse;

    @Parameterized.Parameter(2)
    public Goal.Params cardAllShowParams;

    @Parameterized.Parameters(name = "{index}. {0}. Отрктыие карточки оффера")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", mockOffer(SELL_APARTMENT).setService(PROMOTED),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .flatType(SECONDARY)
                                .page(OFFER)
                                .payed(TRUE)
                                .hasEgrnReport(TRUE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .planType(SELECTED)
                                .primarySale(false)
                                .tuz(FALSE)
                                .vas(asList(PROMOTION))},
                {"Купить новостроечную вторичку",
                        mockOffer(SELL_NEW_BUILDING_SECONDARY)
                                .setExtImages(),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .flatType(NEW_FLAT)
                                .page(OFFER)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .planType(OWN)
                                .primarySale(true)
                                .tuz(FALSE)
                                .payed(FALSE)},
                {"Купить новую вторичку",
                        mockOffer(SELL_NEW_SECONDARY)
                                .setPredictions(),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(TRUE)
                                .hasPlan(FALSE)
                                .flatType(NEW_SECONDARY)
                                .page(OFFER)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .primarySale(false)
                                .onlineShowAvailable(FALSE)
                                .tuz(FALSE)
                                .payed(FALSE)},
                {"Купить комнату",
                        mockOffer(SELL_ROOM)
                                .setService(PROMOTED),
                        params().offerType(SELL)
                                .offerCategory(ROOMS)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .page(OFFER)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .tuz(TRUE)
                                .payed(TRUE)
                                .vas(asList(PROMOTION))},
                {"Купить дом",
                        mockOffer(SELL_HOUSE)
                                .setService(RAISED),
                        params().offerType(SELL)
                                .offerCategory(HOUSE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .page(OFFER)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .primarySale(false)
                                .onlineShowAvailable(FALSE)
                                .tuz(FALSE)
                                .payed(TRUE)
                                .vas(asList(RAISING))},
                {"Купить участок",
                        mockOffer(SELL_LOT),
                        params().offerType(SELL)
                                .offerCategory(LOT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .page(OFFER)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .primarySale(false)
                                .tuz(FALSE)
                                .payed(FALSE)},
                {"Купить гараж",
                        mockOffer(SELL_GARAGE),
                        params().offerType(SELL)
                                .offerCategory(GARAGE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .page(OFFER)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .tuz(FALSE)
                                .payed(FALSE)},
                {"Купить коммерческую",
                        mockOffer(SELL_COMMERCIAL)
                                .setService(TURBOSALE)
                                .setExtImages(),
                        params().offerType(SELL)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .page(OFFER)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .planType(OWN)
                                .tuz(FALSE)
                                .payed(TRUE)
                                .vas(asList(TURBO))},
                {"Снять квартиру",
                        mockOffer(RENT_APARTMENT)
                                .setService(PREMIUM).setService(PROMOTED).setExtImages(),
                        params()
                                .offerType(RENT)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .page(OFFER)
                                .pricingPeriod(PER_MONTH)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .planType(OWN)
                                .onlineShowAvailable(FALSE)
                                .tuz(TRUE)
                                .payed(TRUE)
                                .vas(asList(PREMIUM, PROMOTION))},
                {"Снять комнату",
                        mockOffer(RENT_ROOM)
                                .setPredictions(),
                        params()
                                .offerType(RENT)
                                .offerCategory(ROOMS)
                                .hasGoodPrice(TRUE)
                                .hasPlan(FALSE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .pricingPeriod(PER_MONTH)
                                .tuz(FALSE)
                                .payed(FALSE)
                                .page(OFFER)},
                {"Снять дом",
                        mockOffer(RENT_HOUSE),
                        params()
                                .offerType(RENT)
                                .offerCategory(HOUSE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .pricingPeriod(PER_MONTH)
                                .tuz(FALSE)
                                .payed(FALSE)
                                .page(OFFER)},
                {"Снять гараж",
                        mockOffer(RENT_GARAGE),
                        params()
                                .offerType(RENT)
                                .offerCategory(GARAGE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .pricingPeriod(PER_MONTH)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .tuz(FALSE)
                                .payed(FALSE)
                                .page(OFFER)},
                {"Снять коммерческую",
                        mockOffer(RENT_COMMERCIAL_WAREHOUSE)
                                .setService(PREMIUM),
                        params()
                                .offerType(RENT)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .page(OFFER)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .pricingPeriod(PER_MONTH)
                                .tuz(FALSE)
                                .payed(TRUE)
                                .vas(asList(PREMIUM))},
        });
    }

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeOfferGoalsWithoutAll() {
        cardAllShowParams.offerId(cardOfferResponse.getOfferId());
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(cardOfferResponse)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(cardOfferResponse).build()).createWithDefaults();
        proxy.clearHar();
        urlSteps.testing().path(Pages.OFFER).path(cardOfferResponse.getOfferId()).open();
        goalsSteps.urlMatcher(containsString(CARD_ALL_SHOW))
                .withGoalParams(goal().setCard(cardAllShowParams)).shouldExist();
    }
}