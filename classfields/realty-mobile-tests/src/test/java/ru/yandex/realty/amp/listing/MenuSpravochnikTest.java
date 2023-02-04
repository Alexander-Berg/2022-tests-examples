package ru.yandex.realty.amp.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
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
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MAGADAN;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Link("VERTISTEST-1618")
@Feature(AMP_FEATURE)
@DisplayName("amp. Ссылки на Справочник в меню")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MenuSpravochnikTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String region;

    @Parameterized.Parameters(name = "Ссылка «Справочник» в меню, регион «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Москва", MOSKVA},
                {"Санкт-Петербург", SANKT_PETERBURG},
                {"Магадан", MAGADAN}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ссылки «Справочник» в меню")
    public void shouldSeeSpravochnikUrlMenuAmp() {
        urlSteps.testing().path(AMP).path(region).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link("Журнал").should(hasHref(equalTo(
                urlSteps.testing().path("/journal/").toString())));
    }
}
