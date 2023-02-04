package ru.yandex.realty.menu;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import static ru.yandex.realty.consts.Filters.MAGADAN;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.MENU;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Issue("VERTISTEST-1352")
@Feature(MENU)
@DisplayName("Ссылки на Мои объявления в меню")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MenuMyOffersLinkTest {

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

    @Parameterized.Parameters(name = "Ссылка Мои объявления в меню, регион «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Москва", MOSKVA},
                {"Санкт-Петербург", SANKT_PETERBURG},
                {"Магадан", MAGADAN}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки «Мои объявления» в меню")
    public void shouldSeeMyOffersUrlMenu() {
        urlSteps.testing().path(region).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link("Мои объявления").should(hasHref(equalTo(
                "https://bzfk.adj.st/management-new/?adjust_t=wsudr1e_e8tc29m&adjust_campaign=&adjust_adgroup=&" +
                        "adjust_creative=&adjust_fallback=&adjust_deeplink=yandexrealty%3A%2F%2F" +
                        "realty.yandex.ru%2Fmanagement-new%2F")));

    }
}
