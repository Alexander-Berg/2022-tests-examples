package ru.yandex.realty.touchgoals.favorites.add.other;

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
import ru.yandex.realty.consts.GoalsConsts;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebWithProxyMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_ADD_GOAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.CALCULATOR;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.MORTGAGE_CALCULATOR;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RAISING;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SUITABLE_OFFERS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TURBO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA_CALCULATOR;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.mock.MockOffer.RAISED;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.SELL_NEW_BUILDING_SECONDARY;
import static ru.yandex.realty.mock.MockOffer.TURBOSALE;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «favorites.add». Другие страницы")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyMobileModule.class)
public class FavoritesOtherPagesGoalsTest {


    private static final String PRICE_MAX = "priceMax";
    private static final String PRICE_MAX_VALUE = "80000000";
    private static final String NEW_FLAT = "newFlat";

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

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Страница ипотеки. Все")
    public void shouldSeeMortgageFavoritesAddGoal() {
        MockOffer offer = mockOffer(SELL_APARTMENT).setPredictions().setService(TURBOSALE);
        mockRuleConfigurable.offerWithSiteSearchStub(
                offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .createWithDefaults();
        urlSteps.testing().path(IPOTEKA_CALCULATOR).queryParam(PRICE_MAX, PRICE_MAX_VALUE).open();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onMortgagePage().offer(FIRST).addToFav().click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(params()
                        .offerType(SELL)
                        .offerCategory(APARTMENT)
                        .hasGoodPrice(TRUE)
                        .hasPlan(TRUE)
                        .flatType(SECONDARY)
                        .placement(SUITABLE_OFFERS)
                        .pageType(CALCULATOR)
                        .vas(asList(TURBO))
                        .page(MORTGAGE_CALCULATOR))).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Страница ипотеки. Вторичка")
    public void shouldSeeSecondaryMortgageFavoritesAddGoal() {
        MockOffer offer = mockOffer(SELL_APARTMENT).setService(RAISED);
        mockRuleConfigurable.offerWithSiteSearchStub(
                offerWithSiteSearchTemplate().offers(asList(offer)).build()).createWithDefaults();
        urlSteps.testing().path(IPOTEKA_CALCULATOR).queryParam(NEW_FLAT, "NO").queryParam(PRICE_MAX, PRICE_MAX_VALUE).open();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onMortgagePage().offer(FIRST).addToFav().click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(params()
                        .offerType(SELL)
                        .offerCategory(APARTMENT)
                        .hasGoodPrice(FALSE)
                        .hasPlan(TRUE)
                        .flatType(SECONDARY)
                        .placement(SUITABLE_OFFERS)
                        .pageType(CALCULATOR)
                        .vas(asList(RAISING))
                        .page(MORTGAGE_CALCULATOR))).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Страница ипотеки. Новостройки")
    public void shouldSeeNewBuildingMortgageFavoritesAddGoal() {
        MockOffer offer = mockOffer(SELL_NEW_BUILDING_SECONDARY).setExtImages();
        mockRuleConfigurable.offerWithSiteSearchStub(
                offerWithSiteSearchTemplate().offers(asList(offer)).build()).createWithDefaults();
        urlSteps.testing().path(IPOTEKA_CALCULATOR).queryParam(NEW_FLAT, "YES").queryParam(PRICE_MAX, PRICE_MAX_VALUE).open();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onMortgagePage().offer(FIRST).addToFav().click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(params()
                        .offerType(SELL)
                        .offerCategory(APARTMENT)
                        .hasGoodPrice(FALSE)
                        .hasPlan(TRUE)
                        .flatType(GoalsConsts.Parameters.NEW_FLAT)
                        .placement(SUITABLE_OFFERS)
                        .pageType(CALCULATOR)
                        .page(MORTGAGE_CALCULATOR))).shouldExist();
    }
}
