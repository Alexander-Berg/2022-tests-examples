package ru.yandex.realty.goals.phoneallshow.card.offer;

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
import ru.yandex.realty.consts.GoalsConsts;
import ru.yandex.realty.consts.Pages;
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
import static ru.yandex.realty.consts.GoalsConsts.Goal.PHONE_ALL_SHOW;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.CARD;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.COMMERCIAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.GARAGE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER_BASE_INFO;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PER_MONTH;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PROMOTION;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.ROOMS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.PREMIUM;
import static ru.yandex.realty.mock.MockOffer.PROMOTED;
import static ru.yandex.realty.mock.MockOffer.RENT_COMMERCIAL;
import static ru.yandex.realty.mock.MockOffer.RENT_GARAGE;
import static ru.yandex.realty.mock.MockOffer.SELL_GARAGE;
import static ru.yandex.realty.mock.MockOffer.SELL_ROOM;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferPhonesResponse.TEST_PHONE;
import static ru.yandex.realty.mock.OfferPhonesResponse.offersPhonesTemplate;

@DisplayName("Цель «phone.all.show». Каротчка оффера")
@Feature(GOALS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PhoneShowBaseInfoGoalsTest {

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
    public MockOffer offer;

    @Parameterized.Parameter(2)
    public Goal.Params phoneAllShowParams;

    @Parameterized.Parameters(name = "{index}. {0}. Показать телефон")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Купить комнату",
                        mockOffer(SELL_ROOM)
                                .setService(PROMOTED),
                        params().offerType(SELL)
                                .offerCategory(ROOMS)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .tuz(TRUE)
                                .payed(TRUE)
                                .vas(asList(PROMOTION))},
                {"Купить гараж",
                        mockOffer(SELL_GARAGE),
                        params().offerType(SELL)
                                .offerCategory(GARAGE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .tuz(FALSE)
                                .payed(FALSE)
                                .page(OFFER)},
                {"Снять гараж",
                        mockOffer(RENT_GARAGE),
                        params().offerType(RENT)
                                .offerCategory(GARAGE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pricingPeriod(PER_MONTH)
                                .pageType(CARD)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .tuz(FALSE)
                                .page(OFFER)
                                .payed(FALSE)},
                {"Снять коммерческую",
                        mockOffer(RENT_COMMERCIAL)
                                .setService(PREMIUM),
                        params().offerType(RENT)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pricingPeriod(PER_MONTH)
                                .pageType(CARD)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .tuz(FALSE)
                                .page(OFFER)
                                .payed(TRUE)
                                .vas(asList(GoalsConsts.Parameters.PREMIUM))},
        });
    }

    @Before
    public void before() {
        phoneAllShowParams.offerId(offer.getOfferId());
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate().addPhone(TEST_PHONE).build())
                .createWithDefaults();
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        proxy.clearHarUntilThereAreNoHarEntries();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("В карточке основной информации")
    public void shouldSeeBaseInfoPhoneAllShowAddGoal() {
        basePageSteps.onOfferCardPage().offerCardSummary().showPhoneButton().click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneAllShowParams)).shouldExist();
    }
}