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
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.IPOTEKA;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

/**
 * @author kantemirov
 */
@DisplayName("Фильтры ипотеки")
@Feature(MORTGAGE)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class IpotekaPresetsTest {

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
                {"калькулятор", "/ipoteka/calculator/"},
                {"вторичное жилье", "/ipoteka/?flatType=SECONDARY"},
                {"новостройки", "/ipoteka/?flatType=NEW_FLAT"},
                {"Господдержка", "/ipoteka/s-gospodderzhkoi/"},
                {"Без подтверждения", "/ipoteka/bez-podtverzhdeniya-dohoda/"},
                {"Семейная", "/ipoteka/semeinaya-ipoteka/"},
                {"бизнесмена или ИП",
                        "/ipoteka/?borrowerCategory=BUSINESS_OWNER&borrowerCategory=INDIVIDUAL_ENTREPRENEUR"},
                {"Материнский", "/ipoteka/matkapital/"},
        });
    }

    @Before
    public void before() {
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Пресеты ипотек")
    public void shouldSeeMoscowFilter() {
        urlSteps.setMoscowCookie();
        urlSteps.testing().path(IPOTEKA).open();
        basePageSteps.onIpotekaPage().filters().preset(label).link().should(hasHref(containsString(expected)));
    }
}