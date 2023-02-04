package ru.yandex.realty.newbuilding.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.REAL_PRODUCTION;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@Issue("VERTISTEST-1352")
@Epic(NEWBUILDING)
@DisplayName("Баннер спецпроекта")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class PikBannerNewbuildingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.clearCookie("isAdDisabledTest");
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход по баннеру спецпроекта в новостройках. Москва")
    public void shouldGoSpecProjectPageMoskva() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).open();
        basePageSteps.onNewBuildingPage().topAddBanner().click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(REAL_PRODUCTION).path(SAMOLET).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Issue("Чинить будут тут https://st.yandex-team.ru/REALTYFRONT-14204")
    @DisplayName("Переход по баннеру спецпроекта в новостройках. Спб")
    public void shouldGoSpecProjectPageSpb() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).open();
        basePageSteps.onNewBuildingPage().topAddBanner().click();
        basePageSteps.switchToNextTab();

        urlSteps.fromUri(REAL_PRODUCTION).path(SPB_I_LO).path(SAMOLET).shouldNotDiffWithWebDriverUrl();
    }
}
