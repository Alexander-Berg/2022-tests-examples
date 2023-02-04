package ru.yandex.realty.ipoteka;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;
import static ru.yandex.realty.page.IpotekaPage.ANY_PROGRAM_BUTTON;

/**
 * @author kantemirov
 */
@Link("https://st.yandex-team.ru/VERTISTEST-1652")
@DisplayName("Фильтры ипотеки")
@Feature(MORTGAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class IpotekaProgramsFiltersTest {

    private static final String MORTGAGE_TYPE = "mortgageType";
    private static final String STANDARD = "STANDARD";
    private static final String STANDART = "Стандартная";
    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фильтры программ ипотек в Москве[Стандартная]")
    public void shouldSeeMoscowFilter() {
        urlSteps.setMoscowCookie();
        urlSteps.testing().path(IPOTEKA).open();
        basePageSteps.onIpotekaPage().filters().button(ANY_PROGRAM_BUTTON).click();
        basePageSteps.onIpotekaPage().filters().selectorItem(STANDART).click();
        urlSteps.queryParam(MORTGAGE_TYPE, STANDARD).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фильтры программ ипотек в СПб[Стандартная]")
    public void shouldSeeSpbFilter() {
        urlSteps.setSpbCookie();
        urlSteps.testing().path(SPB_I_LO).path(IPOTEKA).open();
        basePageSteps.onIpotekaPage().filters().button(ANY_PROGRAM_BUTTON).click();
        basePageSteps.onIpotekaPage().filters().selectorItem(STANDART).click();
        urlSteps.queryParam(MORTGAGE_TYPE, STANDARD).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем 2 программы")
    public void shouldSeeTwoPrograms() {
        urlSteps.setMoscowCookie();
        urlSteps.testing().path(IPOTEKA).open();
        basePageSteps.onIpotekaPage().filters().button(ANY_PROGRAM_BUTTON).click();
        basePageSteps.onIpotekaPage().filters().selectorItem("С господдержкой").click();
        basePageSteps.onIpotekaPage().filters().selectorItem("Военная ипотека").click();
        urlSteps.queryParam(MORTGAGE_TYPE, "STATE_SUPPORT").queryParam(MORTGAGE_TYPE, "MILITARY")
                .shouldNotDiffWithWebDriverUrl();
    }
}