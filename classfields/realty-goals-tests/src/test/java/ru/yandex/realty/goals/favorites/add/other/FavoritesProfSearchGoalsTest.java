package ru.yandex.realty.goals.favorites.add.other;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Description;
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
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_ADD_GOAL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.CARD;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEWBUILDING;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP_PROFSEARCH_ITEM;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
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
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.page.ProfSearchPage.INTO_FAV;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «favorites.add». Профпоиск")
@Feature(GOALS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebWithProxyModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoritesProfSearchGoalsTest {

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
    public Goal.Params favoriteAddParams;

    @Parameterized.Parameters(name = "{index}. {0}. Добавить в избранное")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", "SELL", "APARTMENT",
                        mockOffer(SELL_APARTMENT),
                        params()
                                .placement(SERP_PROFSEARCH_ITEM)
                                .page(NEWBUILDING)
                                .pageType(CARD)},
                {"Купить дом", "SELL", "HOUSE",
                        mockOffer(SELL_HOUSE)
                                .setService(RAISED),
                        params()
                                .placement(SERP_PROFSEARCH_ITEM)
                                .page(NEWBUILDING)
                                .pageType(CARD)},
                {"Купить коммерческую", "SELL", "COMMERCIAL",
                        mockOffer(SELL_COMMERCIAL)
                                .setService(TURBOSALE)
                                .setExtImages(),
                        params()
                                .placement(SERP_PROFSEARCH_ITEM)
                                .page(NEWBUILDING)
                                .pageType(CARD)},
                {"Снять квартиру", "RENT", "APARTMENT",
                        mockOffer(RENT_APARTMENT)
                                .setService(PREMIUM)
                                .setService(PROMOTED)
                                .setExtImages(),
                        params()
                                .placement(SERP_PROFSEARCH_ITEM)
                                .page(NEWBUILDING)
                                .pageType(CARD)},
                {"Снять коммерческую", "RENT", "COMMERCIAL",
                        mockOffer(RENT_COMMERCIAL)
                                .setService(PREMIUM),
                        params()
                                .placement(SERP_PROFSEARCH_ITEM)
                                .page(NEWBUILDING)
                                .pageType(CARD)},
        });
    }

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .createWithDefaults();
        apiSteps.createVos2Account(account, AccountType.OWNER);
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).queryParam("type", type).queryParam("category", category)
                .open();
    }

    @Description("ВОЗМОЖНО БАГ УХОДИТ ««favorite»» ВМЕСТО ««favorites»», и вообще уходит одна и та же цель, " +
            "но решили, что сейчас поведение ожидаемое")
    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeListingFavoritesAddGoal() {
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.scroll(100);
        basePageSteps.onProfSearchPage().offer(FIRST).spanLink(INTO_FAV).click();
        goalsSteps.urlMatcher(containsString(FAVORITES_ADD_GOAL)).withGoalParams(
                goal().setFavorite(favoriteAddParams))
                .shouldExist();
    }
}