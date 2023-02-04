package ru.yandex.realty.goals.favorites.add.listing;

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
import ru.yandex.realty.beans.Goal;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_ADD_GOAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFERS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP_OFFERS_ITEM;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.element.saleads.ActionBar.ADD_TO_FAV;
import static ru.yandex.realty.element.saleads.ActionBar.YOUR_NOTE;
import static ru.yandex.realty.mock.MockOffer.RENT_BY_DAY;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «favorites.add». Листинг")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class FavoritesListingGoalsRentByDayTest {

    private Goal.Params favoritesAddParams;

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
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(mockOffer(RENT_BY_DAY))).build())
                .createWithDefaults();

        urlSteps.testing().path(SANKT_PETERBURG).path(SNYAT).path(KVARTIRA).queryParam("rentTime", "SHORT").open();
        proxy.clearHarUntilThereAreNoHarEntries();
        favoritesAddParams = params()
                .offerType(RENT)
                .offerCategory(APARTMENT)
                .hasGoodPrice(FALSE)
                .hasPlan(FALSE)
                .pageType(SERP)
                .placement(SERP_OFFERS_ITEM)
                .page(OFFERS);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Снять кварттиру посуточно. Клик «Добавить в избранное»")
    public void shouldSeeListingByDayFavoritesAddGoal() {
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(ADD_TO_FAV).click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL))
                .withGoalParams(goal().setFavorites(favoritesAddParams)).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Снять кварттиру посуточно. Добавить заметку")
    public void shouldSeeListingByDayFavoritesAddNoteGoal() {
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offer(FIRST));
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(YOUR_NOTE).click();
        basePageSteps.onOffersSearchPage().offer(FIRST).addNoteField().input().sendKeys(randomAlphabetic(10));
        basePageSteps.onOffersSearchPage().offer(FIRST).saveNote().click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(favoritesAddParams)).shouldExist();
    }
}