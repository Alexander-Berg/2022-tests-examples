package ru.yandex.realty.ipoteka;

import com.carlosbecker.guice.GuiceModules;
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
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class IpotekaRegionsProgramsFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"С господдержкой", "/s-gospodderzhkoi/"},
                {"Семейная ипотека", "/semeinaya-ipoteka/"},
                {"Военная ипотека", "/dlya-voennyh/"},
                {"Дальневосточная ипотека", "/dalnevostochnaya-ipoteka/"},
                {"Целевая ипотека под залог имеющейся недвижимости", "/pod-zalog-nedvizhimosti/"},
        });
    }

    @Before
    public void before() {
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фильтры программ ипотек в Москве")
    public void shouldSeeMoscowFilter() {
        urlSteps.setMoscowCookie();
        urlSteps.testing().path(IPOTEKA).open();
        basePageSteps.onIpotekaPage().filters().button(ANY_PROGRAM_BUTTON).click();
        basePageSteps.onIpotekaPage().filters().selectorItem(label).click();
        urlSteps.path(expected).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фильтры программ ипотек в СПб")
    public void shouldSeeSpbFilter() {
        urlSteps.setSpbCookie();
        urlSteps.testing().path(SPB_I_LO).path(IPOTEKA).open();
        basePageSteps.onIpotekaPage().filters().button(ANY_PROGRAM_BUTTON).click();
        basePageSteps.onIpotekaPage().filters().selectorItem(label).click();
        urlSteps.path(expected).shouldNotDiffWithWebDriverUrl();
    }
}