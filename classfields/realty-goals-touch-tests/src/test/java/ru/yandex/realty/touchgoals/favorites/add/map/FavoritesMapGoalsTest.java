package ru.yandex.realty.touchgoals.favorites.add.map;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebWithProxyMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.GARAZH;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KOMNATA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_ADD_GOAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.COMMERCIAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.GARAGE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.HOUSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.LOT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.MAP;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.MAPSERP_OFFERS_ITEM;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEW_FLAT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEW_SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFERS;
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
import static ru.yandex.realty.element.saleads.ActionBar.ADD_TO_FAV;
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
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «favorites.add». Карта")
@Feature(GOALS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithProxyMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoritesMapGoalsTest {

    private static final String ANY_ACTIVE_POINT_COORDINATES = "60.0%2C30.0";

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
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS)},
                {"Купить новостроечную вторичку", KUPIT + KVARTIRA,
                        mockOffer(SELL_NEW_BUILDING_SECONDARY)
                                .setExtImages(),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .flatType(NEW_FLAT)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS)},
                {"Купить новую вторичку", KUPIT + KVARTIRA,
                        mockOffer(SELL_NEW_SECONDARY)
                                .setPredictions(),
                        params().offerType(SELL)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(TRUE)
                                .hasPlan(FALSE)
                                .flatType(NEW_SECONDARY)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS)},
                {"Купить комнату", KUPIT + KOMNATA,
                        mockOffer(SELL_ROOM)
                                .setService(PROMOTED),
                        params().offerType(SELL)
                                .offerCategory(ROOMS)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS)
                                .vas(asList(PROMOTION))},
                {"Купить дом", KUPIT + DOM,
                        mockOffer(SELL_HOUSE)
                                .setService(RAISED),
                        params().offerType(SELL)
                                .offerCategory(HOUSE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS)
                                .vas(asList(RAISING))},
                {"Купить участок", KUPIT + UCHASTOK,
                        mockOffer(SELL_LOT),
                        params().offerType(SELL)
                                .offerCategory(LOT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS)},
                {"Купить гараж", KUPIT + GARAZH,
                        mockOffer(SELL_GARAGE),
                        params().offerType(SELL)
                                .offerCategory(GARAGE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS)},
                {"Купить коммерческую", KUPIT + Filters.COMMERCIAL,
                        mockOffer(SELL_COMMERCIAL)
                                .setService(TURBOSALE)
                                .setExtImages(),
                        params().offerType(SELL)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(COMMERCIAL)
                                .vas(asList(TURBO))},
                {"Снять квартиру", SNYAT + KVARTIRA,
                        mockOffer(RENT_APARTMENT)
                                .setService(PREMIUM)
                                .setService(PROMOTED)
                                .setExtImages(),
                        params().offerType(RENT)
                                .offerCategory(APARTMENT)
                                .hasGoodPrice(FALSE)
                                .hasPlan(TRUE)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS).vas(asList(GoalsConsts.Parameters.PREMIUM, PROMOTION))},
                {"Снять комнату", SNYAT + KOMNATA,
                        mockOffer(RENT_ROOM)
                                .setPredictions(),
                        params().offerType(RENT)
                                .offerCategory(ROOMS)
                                .hasGoodPrice(TRUE)
                                .hasPlan(FALSE)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS)},
                {"Снять дом", SNYAT + DOM,
                        mockOffer(RENT_HOUSE),
                        params().offerType(RENT)
                                .offerCategory(HOUSE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS)},
                {"Снять гараж", SNYAT + GARAZH,
                        mockOffer(RENT_GARAGE),
                        params().offerType(RENT)
                                .offerCategory(GARAGE)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(OFFERS)},
                {"Снять коммерческую", SNYAT + Filters.COMMERCIAL,
                        mockOffer(RENT_COMMERCIAL)
                                .setService(PREMIUM),
                        params().offerType(RENT)
                                .offerCategory(COMMERCIAL)
                                .hasGoodPrice(FALSE)
                                .hasPlan(FALSE)
                                .placement(MAPSERP_OFFERS_ITEM)
                                .pageType(MAP)
                                .page(COMMERCIAL)
                                .vas(asList(GoalsConsts.Parameters.PREMIUM))},
        });
    }

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .createWithDefaults();
        urlSteps.testing().path(SANKT_PETERBURG).path(path).path(KARTA)
                .queryParam("activePointId", ANY_ACTIVE_POINT_COORDINATES).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeMapFavoritesAddGoal() {
        proxy.clearHarUntilThereAreNoHarEntries();

        basePageSteps.onMobileMapPage().offer(FIRST).buttonWithTitle(ADD_TO_FAV).click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(favoritesAddParams)).shouldExist();
    }
}
