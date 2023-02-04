package ru.yandex.realty.touchgoals.favorites.open;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebWithProxyMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.GoalsConsts.Goal.FAVORITES_OPEN;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.BURGER;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;

@DisplayName("Цель «favorites.open».")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyMobileModule.class)
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
    @DisplayName("Главное меню")
    public void shouldSeeFavoritesOpenInMainMenu() {
        urlSteps.testing().open();
        basePageSteps.onBasePage().menuButton().click();
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onBasePage().menu().link("Избранное").click();
        goalsSteps.urlMatcher(containsString(FAVORITES_OPEN)).withGoalParams(
                goal().setFavorites(params()
                        .source(BURGER))).shouldExist();
    }
}
