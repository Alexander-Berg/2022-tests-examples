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
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.GoalsConsts.Goal.PHONE_ALL_SHOW;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEWBUILDING;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.SERP_NEWBUILDINGS_ITEM;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.Location.MOSCOW_AND_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «phone.all.show». Листинг новостроек")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class PhoneShowNewBuildingListingGoalsTest {

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
        phoneAllShowParams = params()
                .placement(SERP_NEWBUILDINGS_ITEM)
                .pageType(SERP)
                .page(NEWBUILDING)
                .payed(TRUE);
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        urlSteps.testing().path(MOSCOW_AND_MO.getPath()).path(KUPIT).path(NOVOSTROJKA).open();

    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Листинг новостроек")
    public void shouldSeeBaseInfoPhoneAllAddGoal() {
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onNewBuildingPage().offer(FIRST).showPhoneButton().click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneAllShowParams)).shouldExist();
    }
}