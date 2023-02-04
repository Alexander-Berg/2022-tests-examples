package ru.yandex.realty.goals.favorites.add.other;

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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.mock.OfferWithSiteSearchResponse;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static java.util.Arrays.asList;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_ADD_GOAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.COMMERCIAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.OTHER;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PROFSEARCH;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PROFSEARCH_GROUP_FAVORITE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.mock.MockOffer.SELL_COMMERCIAL;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.page.ProfSearchPage.INTO_FAV;

@DisplayName("Цель «favorites.add». Профпоиск")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class FavoritesProfSearchBatchGoalTest {

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

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавление в избранное нескольких объявлений сразу")
    public void shouldSeeProfSearchBatchFavoritesAddGoal() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        apiSteps.createVos2Account(account, AccountType.OWNER);
        MockOffer firstOffer = mockOffer(SELL_COMMERCIAL);
        MockOffer secondOffer = mockOffer(SELL_COMMERCIAL);
        OfferWithSiteSearchResponse response = offerWithSiteSearchTemplate().offers(asList(firstOffer, secondOffer));
        mockRuleConfigurable.offerWithSiteSearchStub(response.build()).createWithDefaults();
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).open();
        basePageSteps.scroll(100);
        basePageSteps.onProfSearchPage().checkAll().click();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onProfSearchPage().groupAction().button(INTO_FAV).click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorites(params()
                        .offerType(SELL)
                        .offerCategory(COMMERCIAL)
                        .hasGoodPrice(FALSE)
                        .hasPlan(FALSE)
                        .placement(PROFSEARCH_GROUP_FAVORITE)
                        .page(PROFSEARCH)
                        .pageType(OTHER))).shouldExist();
    }
}