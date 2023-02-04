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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.beans.Goal;
import ru.yandex.realty.consts.GoalsConsts;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import java.util.Collection;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.GoalsConsts.Goal.PHONE_ALL_SHOW;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.COMMERCIAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.HOUSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PER_MONTH;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PROFSEARCH;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PROMOTION;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RAISING;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP_PROFSEARCH_ITEM;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TURBO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.PREMIUM;
import static ru.yandex.realty.mock.MockOffer.PROMOTED;
import static ru.yandex.realty.mock.MockOffer.RAISED;
import static ru.yandex.realty.mock.MockOffer.RENT_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.RENT_COMMERCIAL;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.SELL_COMMERCIAL;
import static ru.yandex.realty.mock.MockOffer.SELL_HOUSE;
import static ru.yandex.realty.mock.MockOffer.TURBOSALE;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferPhonesResponse.TEST_PHONE;
import static ru.yandex.realty.mock.OfferPhonesResponse.offersPhonesTemplate;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «phone.all.show». Профпоиск")
@Feature(GOALS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PhoneShowProfSearchGoalsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private ProxySteps proxy;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String type;

    @Parameterized.Parameter(2)
    public String category;

    @Parameterized.Parameter(3)
    public MockOffer offer;

    @Parameterized.Parameter(4)
    public Goal.Params params;

    @Parameterized.Parameters(name = "{index}. {0}. Добавить в избранное")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", "SELL", "APARTMENT",
                        mockOffer(SELL_APARTMENT),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .flatType(SECONDARY)
                                .placement(SERP_PROFSEARCH_ITEM)
                                .pageType(SERP)
                                .page(PROFSEARCH)
                                .exactMatch(TRUE)
                                .hasEgrnReport(TRUE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(FALSE)
                                .primarySale(false)
                                .tuz(FALSE)
                },
                {"Купить дом", "SELL", "HOUSE",
                        mockOffer(SELL_HOUSE)
                                .setService(RAISED),
                        params().offerType(SELL)
                                .offerCategory(HOUSE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_PROFSEARCH_ITEM)
                                .pageType(SERP)
                                .page(PROFSEARCH)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(TRUE)
                                .primarySale(false)
                                .tuz(FALSE)
                                .vas(asList(RAISING))},
                {"Купить коммерческую", "SELL", "COMMERCIAL",
                        mockOffer(SELL_COMMERCIAL)
                                .setService(TURBOSALE).setExtImages(),
                        params().offerType(SELL)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .placement(SERP_PROFSEARCH_ITEM)
                                .pageType(SERP)
                                .page(PROFSEARCH)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(TRUE)
                                .tuz(FALSE)
                                .vas(asList(TURBO))},
                {"Снять квартиру", "RENT", "APARTMENT",
                        mockOffer(RENT_APARTMENT)
                                .setService(PREMIUM).setService(PROMOTED).setExtImages(),
                        params().offerType(RENT)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .placement(SERP_PROFSEARCH_ITEM)
                                .pageType(SERP)
                                .page(PROFSEARCH)
                                .pricingPeriod(PER_MONTH)
                                .exactMatch(TRUE)
                                .hasEgrnReport(FALSE)
                                .hasVideoReview(FALSE)
                                .hasVirtualTour(FALSE)
                                .onlineShowAvailable(FALSE)
                                .payed(TRUE)
                                .tuz(TRUE)
                                .vas(asList(GoalsConsts.Parameters.PREMIUM, PROMOTION))},
                {"Снять коммерческую", "RENT", "COMMERCIAL",
                        mockOffer(RENT_COMMERCIAL)
                                .setService(PREMIUM),
                        params().offerType(RENT)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(SERP_PROFSEARCH_ITEM)
                                .pageType(SERP)
                                .page(PROFSEARCH)
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
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        mockRuleConfigurable
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate().addPhone(TEST_PHONE).build())
                .createWithDefaults();
        apiSteps.createVos2Account(account, AccountType.OWNER);
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).queryParam("type", type).queryParam("category", category)
                .open();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeListingFavoritesAddGoal() {
        basePageSteps.scroll(100);
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onProfSearchPage().offer(FIRST).showPhoneButton().click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(params))
                .shouldExist();
    }
}