package ru.yandex.realty.goals.favorites.add.card;

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
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.GARAZH;
import static ru.yandex.realty.consts.Filters.KOMNATA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_ADD_GOAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.CARD;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.COMMERCIAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.GARAGE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.HOUSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.LOT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEW_FLAT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEW_SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFER_BASE_INFO;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PROMOTION;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RAISING;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.ROOMS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
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

@DisplayName("Цель «favorites.add». Карточка оффера")
@Feature(GOALS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoritesCardGoalsTest {

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
    public MockOffer cardOffer;

    @Parameterized.Parameter(3)
    public Goal.Params favoritesAddParams;

    @Parameterized.Parameters(name = "{index}. {0}. Добавить в избранное")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", KUPIT + KVARTIRA,
                        mockOffer(SELL_APARTMENT),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .flatType(SECONDARY)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)},
                {"Купить новостроечную вторичку", KUPIT + KVARTIRA,
                        mockOffer(SELL_NEW_BUILDING_SECONDARY)
                                .setExtImages(),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .flatType(NEW_FLAT)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)},
                {"Купить новую вторичку", KUPIT + KVARTIRA,
                        mockOffer(SELL_NEW_SECONDARY)
                                .setPredictions(),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(TRUE)
                                .hasPlan(FALSE)
                                .flatType(NEW_SECONDARY)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)},
                {"Купить комнату", KUPIT + KOMNATA,
                        mockOffer(SELL_ROOM)
                                .setService(PROMOTED),
                        params().offerType(SELL)
                                .offerCategory(ROOMS)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)
                                .vas(asList(PROMOTION))},
                {"Купить дом", KUPIT + DOM,
                        mockOffer(SELL_HOUSE)
                                .setService(RAISED),
                        params().offerType(SELL)
                                .offerCategory(HOUSE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)
                                .vas(asList(RAISING))},
                {"Купить участок", KUPIT + UCHASTOK,
                        mockOffer(SELL_LOT),
                        params().offerType(SELL)
                                .offerCategory(LOT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)},
                {"Купить гараж", KUPIT + GARAZH,
                        mockOffer(SELL_GARAGE),
                        params().offerType(SELL)
                                .offerCategory(GARAGE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)},
                {"Купить коммерческую", KUPIT + Filters.COMMERCIAL,
                        mockOffer(SELL_COMMERCIAL)
                                .setService(TURBOSALE)
                                .setExtImages(),
                        params().offerType(SELL)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)
                                .vas(asList(TURBO))},
                {"Снять квартиру", SNYAT + KVARTIRA,
                        mockOffer(RENT_APARTMENT)
                                .setService(PREMIUM).setService(PROMOTED).setExtImages(),
                        params().offerType(RENT)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER).vas(asList(GoalsConsts.Parameters.PREMIUM, PROMOTION))},
                {"Снять комнату", SNYAT + KOMNATA,
                        mockOffer(RENT_ROOM)
                                .setPredictions(),
                        params().offerType(RENT)
                                .offerCategory(ROOMS)
                                .hasGoodPrice(TRUE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)},
                {"Снять дом", SNYAT + DOM,
                        mockOffer(RENT_HOUSE),
                        params().offerType(RENT)
                                .offerCategory(HOUSE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)},
                {"Снять гараж", SNYAT + GARAZH,
                        mockOffer(RENT_GARAGE),
                        params().offerType(RENT)
                                .offerCategory(GARAGE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)},
                {"Снять коммерческую", SNYAT + Filters.COMMERCIAL,
                        mockOffer(RENT_COMMERCIAL_WAREHOUSE)
                                .setService(PREMIUM),
                        params().offerType(RENT)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(OFFER_BASE_INFO)
                                .pageType(CARD)
                                .page(OFFER)
                                .vas(asList(GoalsConsts.Parameters.PREMIUM))},
        });
    }

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(cardOffer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(cardOffer).build()).createWithDefaults();
        urlSteps.testing().path(Pages.OFFER).path(cardOffer.getOfferId()).open();
        proxy.clearHarUntilThereAreNoHarEntries();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeCardFavoritesAddGoal() {
        basePageSteps.onOfferCardPage().offerCardSummary().addToFavButton().click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(favoritesAddParams)).shouldExist();
    }
}