package ru.yandex.realty.goals.phoneallshow.map;

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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.GoalsConsts.Goal.PHONE_ALL_SHOW;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.MAP;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.MAPSERP_NEWBUILDINGS_ITEM;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.NEWBUILDINGS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «phone.all.show». Карта")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class PhoneShowMapGoalsNewBuildingTest {

    private Goal.Params phoneParams;

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
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.onMapPage().wizardTip().closeButton().clickIf(isDisplayed());
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        proxy.clearHarUntilThereAreNoHarEntries();
        phoneParams = params()
                .placement(MAPSERP_NEWBUILDINGS_ITEM)
                .pageType(MAP)
                .page(NEWBUILDINGS)
                .payed(TRUE);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Карта новостроек. Клик «Показать телефон»")
    public void shouldSeeMapPhoneShowGoal() {
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton().click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneParams)).shouldExist();
    }
}