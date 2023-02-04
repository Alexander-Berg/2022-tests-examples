package ru.yandex.realty.ipoteka;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.realty.consts.Owners.CHERNOV;
import static ru.yandex.realty.consts.Pages.IPOTEKA;
import static ru.yandex.realty.consts.RealtyFeatures.MORTGAGE;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

/**
 * @author vovachernov
 */
@DisplayName("Фастлинки ипотеки")
@Feature(MORTGAGE)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class IpotekaFastlinks {

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
                {"калькулятор", "/calculator/"},
                {"вторичное жилье", "/?flatType=SECONDARY"},
                {"новостройки", "/?flatType=NEW_FLAT"},
                {"Господдержка", "/s-gospodderzhkoi/"},
                {"Без подтверждения", "/bez-podtverzhdeniya-dohoda/"},
                {"Семейная", "/semeinaya-ipoteka/"},
                {"бизнесмена или ИП", "/?borrowerCategory=BUSINESS_OWNER&borrowerCategory=INDIVIDUAL_ENTREPRENEUR"},
                {"Материнский", "/matkapital/"}
        });
    }

    @Test
    @Owner(CHERNOV)
    @DisplayName("Фастлинки ипотечных программ")
    public void shouldSeeMoscowFilter() {
        urlSteps.setMoscowCookie();
        urlSteps.testing().path(IPOTEKA).open();
        basePageSteps.onIpotekaPage().fastlinks().fastlink(label).link().should(hasHref(containsString(expected)));
    }
}
