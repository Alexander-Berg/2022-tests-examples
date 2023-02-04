package ru.yandex.realty.touchgoals.favorites.add.map;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebWithProxyMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_ADD_GOAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.MAP;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.MAPSERP_OFFERS_ITEM;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFERS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RENT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.element.saleads.ActionBar.ADD_TO_FAV;
import static ru.yandex.realty.mock.MockOffer.RENT_BY_DAY;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «favorites.add». Карта")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyMobileModule.class)
public class FavoritesMapGoalsRentByDayTest {

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

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Снять квартиру посуточно")
    public void shouldSeeMapFavoritesAddGoal() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(mockOffer(RENT_BY_DAY))).build())
                .createWithDefaults();
        urlSteps.testing().path(SANKT_PETERBURG).path(SNYAT).path(KVARTIRA).path(KARTA).queryParam("rentTime", "SHORT")
                .queryParam("activePointId", ANY_ACTIVE_POINT_COORDINATES).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onMobileMapPage().offer(FIRST).buttonWithTitle(ADD_TO_FAV).click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(params().offerType(RENT)
                        .offerCategory(APARTMENT)
                        .hasGoodPrice(FALSE)
                        .hasPlan(FALSE)
                        .placement(MAPSERP_OFFERS_ITEM)
                        .pageType(MAP)
                        .page(OFFERS))).shouldExist();
    }
}
