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
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_ADD_GOAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OFFERS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.POPUP_PHONE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.RAISING;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SECONDARY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.RAISED;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.page.OffersSearchPage.SHOW_PHONE;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «favorites.add». Листинг")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class FavoritesListingOnOtherPlacementsGoalsTest {

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
    @DisplayName("Клик на «Добавить в избранное» в попапе просмотра телефона")
    public void shouldSeeGoalInShowPhone() {
        MockOffer offer = mockOffer(SELL_APARTMENT).setService(RAISED);
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(SHOW_PHONE).click();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onOffersSearchPage().showPhonePopup().addToFav().click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(params()
                        .offerType(SELL)
                        .flatType(SECONDARY)
                        .offerCategory(APARTMENT)
                        .hasGoodPrice(FALSE)
                        .hasPlan(FALSE)
                        .placement(POPUP_PHONE)
                        .pageType(SERP)
                        .vas(asList(RAISING))
                        .page(OFFERS))).shouldExist();
    }
}