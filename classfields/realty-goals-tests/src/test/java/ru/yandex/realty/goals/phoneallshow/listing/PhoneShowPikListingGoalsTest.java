package ru.yandex.realty.goals.phoneallshow.listing;

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
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.GoalsConsts.Goal.PHONE_ALL_SHOW;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.APARTMENT;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.FALSE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEWBUILDINGS_PIK;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SELL;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP_PIK_ITEM;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.PIK;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «phone.all.show». Листинг ПИКа")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class PhoneShowPikListingGoalsTest {

    private Goal.Params phoneAllShowParams;

    @Rule
    @Inject
    public RuleChain defaultRules;

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
        phoneAllShowParams = params().offerType(SELL)
                .offerCategory(APARTMENT)
                .hasGoodPrice(FALSE)
                .hasPlan(TRUE)
                .placement(SERP_PIK_ITEM)
                .pageType(SERP)
                .page(NEWBUILDINGS_PIK)
                .payed(FALSE);
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        urlSteps.testing().path(PIK).open();

    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Листинг Самолета")
    public void shouldSeePikShowPhone() {
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onSamoletPage().offer(FIRST).showButton().click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneAllShowParams)).shouldExist();
    }
}