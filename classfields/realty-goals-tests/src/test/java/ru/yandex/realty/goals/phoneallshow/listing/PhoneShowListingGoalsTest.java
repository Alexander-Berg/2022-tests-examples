package ru.yandex.realty.goals.phoneallshow.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.yandex.realty.beans.Goal;
import ru.yandex.realty.consts.Filters;
import ru.yandex.realty.consts.GoalsConsts;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.GARAZH;
import static ru.yandex.realty.consts.Filters.KOMNATA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.GoalsConsts.Goal.PHONE_ALL_SHOW;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.COMMERCIAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.GARAGE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.HOUSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.LOT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEW_FLAT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEW_SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFERS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PER_MONTH;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PROMOTION;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RAISING;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.ROOMS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP_OFFERS_ITEM;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TURBO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.PREMIUM;
import static ru.yandex.realty.mock.MockOffer.PROMOTED;
import static ru.yandex.realty.mock.MockOffer.RAISED;
import static ru.yandex.realty.mock.MockOffer.RENT_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.RENT_COMMERCIAL;
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
import static ru.yandex.realty.mock.OfferPhonesResponse.TEST_PHONE;
import static ru.yandex.realty.mock.OfferPhonesResponse.offersPhonesTemplate;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.page.OffersSearchPage.SHOW_PHONE;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «phone.all.show». Листинг")
@Feature(GOALS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PhoneShowListingGoalsTest {

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
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public MockOffer offer;

    @Parameterized.Parameter(3)
    public Goal.Params phoneAllShowParams;

    @Parameterized.Parameters(name = "{index}. {0}. Показать телефон")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", KUPIT + KVARTIRA,
                        mockOffer(SELL_APARTMENT),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .flatType(SECONDARY)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .page(OFFERS)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(FALSE)
                                .primarySale(false)
                                .tuz(FALSE)
                },
                {"Купить новостроечную вторичку", KUPIT + KVARTIRA,
                        mockOffer(SELL_NEW_BUILDING_SECONDARY)
                                .setExtImages(),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .flatType(NEW_FLAT)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .page(OFFERS)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(FALSE)
                                .primarySale(true)
                                .tuz(FALSE)},
                {"Купить новую вторичку", KUPIT + KVARTIRA,
                        mockOffer(SELL_NEW_SECONDARY)
                                .setPredictions(),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .flatType(NEW_SECONDARY)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .page(OFFERS)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(FALSE)
                                .primarySale(false)
                                .tuz(FALSE)},
                {"Купить комнату", KUPIT + KOMNATA,
                        mockOffer(SELL_ROOM)
                                .setService(PROMOTED),
                        params().offerType(SELL)
                                .offerCategory(ROOMS)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .page(OFFERS)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(TRUE)
                                .tuz(TRUE)
                                .vas(asList(PROMOTION))},
                {"Купить дом", KUPIT + DOM,
                        mockOffer(SELL_HOUSE)
                                .setService(RAISED),
                        params().offerType(SELL)
                                .offerCategory(HOUSE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .page(OFFERS)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(TRUE)
                                .primarySale(false)
                                .tuz(FALSE)
                                .vas(asList(RAISING))},
                {"Купить участок", KUPIT + UCHASTOK,
                        mockOffer(SELL_LOT),
                        params().offerType(SELL)
                                .offerCategory(LOT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .page(OFFERS)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(FALSE)
                                .primarySale(false)
                                .tuz(FALSE)},
                {"Купить гараж", KUPIT + GARAZH,
                        mockOffer(SELL_GARAGE),
                        params().offerType(SELL)
                                .offerCategory(GARAGE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .page(OFFERS)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(FALSE)
                                .tuz(FALSE)},
                {"Купить коммерческую", KUPIT + Filters.COMMERCIAL,
                        mockOffer(SELL_COMMERCIAL)
                                .setService(TURBOSALE).setExtImages(),
                        params().offerType(SELL)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .page(OFFERS)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(TRUE)
                                .tuz(FALSE)
                                .vas(asList(TURBO))},
                {"Снять квартиру", SNYAT + KVARTIRA,
                        mockOffer(RENT_APARTMENT)
                                .setService(PREMIUM).setService(PROMOTED).setExtImages(),
                        params().offerType(RENT)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .pricingPeriod(PER_MONTH)
                                .page(OFFERS)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(TRUE)
                                .tuz(TRUE)
                                .vas(asList(GoalsConsts.Parameters.PREMIUM, PROMOTION))},
                {"Снять комнату", SNYAT + KOMNATA,
                        mockOffer(RENT_ROOM)
                                .setPredictions(),
                        params().offerType(RENT)
                                .offerCategory(ROOMS)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .pricingPeriod(PER_MONTH)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(FALSE)
                                .tuz(FALSE)
                                .page(OFFERS)},
                {"Снять дом", SNYAT + DOM,
                        mockOffer(RENT_HOUSE),
                        params().offerType(RENT)
                                .offerCategory(HOUSE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .pricingPeriod(PER_MONTH)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(FALSE)
                                .tuz(FALSE)
                                .page(OFFERS)},
                {"Снять гараж", SNYAT + GARAZH,
                        mockOffer(RENT_GARAGE),
                        params().offerType(RENT)
                                .offerCategory(GARAGE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .pricingPeriod(PER_MONTH)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(FALSE)
                                .tuz(FALSE)
                                .page(OFFERS)},
                {"Снять коммерческую", SNYAT + Filters.COMMERCIAL,
                        mockOffer(RENT_COMMERCIAL)
                                .setService(PREMIUM),
                        params().offerType(RENT)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_OFFERS_ITEM)
                                .pageType(SERP)
                                .page(OFFERS)
                                .pricingPeriod(PER_MONTH)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(TRUE)
                                .tuz(FALSE)
                                .vas(asList(GoalsConsts.Parameters.PREMIUM))},
        });
    }

    @Before
    public void before() {
        phoneAllShowParams.offerId(offer.getOfferId());
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        mockRuleConfigurable
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate().addPhone(TEST_PHONE).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build()).createWithDefaults();
        urlSteps.testing().path(SANKT_PETERBURG).path(path).open();
        proxy.clearHarUntilThereAreNoHarEntries();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeListingPhoneAllShowGoal() {
        basePageSteps.onOffersSearchPage().offer(FIRST).button(SHOW_PHONE).click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneAllShowParams)).shouldExist();
    }
}