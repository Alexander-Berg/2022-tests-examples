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

@Issue("VERTISTEST-1352, REALTYFRONT-14258")
@Feature(MENU)
@DisplayName("Общие ссылки в меню")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MenuCommonLinksWithRegionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "Ссылка на «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Ипотечный калькулятор", "/ipoteka/calculator/"},
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки в меню Москва")
    public void shouldSeeMenuUrlsMoskva() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(title).should(hasHref(equalTo(
                urlSteps.testing().uri(path).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки в меню Санкт-Петербург")
    public void shouldSeeMenuUrlsSanktPeterburg() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(title).should(hasHref(equalTo(
                urlSteps.testing().uri("/sankt-peterburg_i_leningradskaya_oblast" + path).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки в меню Магадан")
    public void shouldSeeMenuUrlsMagadan() {
        urlSteps.testing().path(MAGADAN).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(title).should(hasHref(equalTo(
                urlSteps.testing().uri("/magadanskaya_oblast" + path).toString())));
    }
}
