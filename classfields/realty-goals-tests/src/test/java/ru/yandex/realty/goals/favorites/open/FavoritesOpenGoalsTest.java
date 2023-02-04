package ru.yandex.realty.goals.favorites.open;

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
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_OPEN;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.HEADER;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.PROFILE_POPUP;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.page.BasePage.FAVORITES;

@DisplayName("Цель «favorites.open».")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class FavoritesOpenGoalsTest {

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

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        proxy.clearHar();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Главный хедер")
    public void shouldSeeFavoritesOpenInHeader() {
        urlSteps.testing().open();
        basePageSteps.onBasePage().headerMain().favoritesButton().click();
        goalsSteps.urlMatcher(containsString(FAVORITES_OPEN)).withGoalParams(
                goal().setFavorites(params()
                        .source(HEADER))).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Попап юзера")
    public void shouldSeeFavoritesOpenInMyOffersPopup() {
        apiSteps.createVos2Account(account, AccountType.OWNER);
        urlSteps.testing().open();
        basePageSteps.moveCursor(basePageSteps.onBasePage().headerMain().userAccount());
        basePageSteps.onBasePage().userNewPopup().link(FAVORITES).click();
        goalsSteps.urlMatcher(containsString(FAVORITES_OPEN)).withGoalParams(
                goal().setFavorites(params()
                        .source(PROFILE_POPUP))).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Профпоиск")
    public void shouldSeeFavoritesOpenInProfSearch() {
        apiSteps.createVos2Account(account, AccountType.OWNER);
        basePageSteps.resize(1600, 1800);
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).open();
        basePageSteps.onManagementNewPage().tab(FAVORITES).click();
        goalsSteps.urlMatcher(containsString(FAVORITES_OPEN)).withGoalParams(
                goal().setFavorites(params()
                        .source(HEADER))).shouldExist();
    }
}