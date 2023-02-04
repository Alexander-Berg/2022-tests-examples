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
@DisplayName("amp. Региональные общие ссылки в меню")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MenuRegionDependsCommonLinksTest {

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
    public static Collection<Object[]> rentType() {
        return asList(new Object[][]{
                {"Купить", "/kupit/kvartira/"},
                {"Снять", "/snyat/kvartira/"},
                {"Коммерческая", "/kupit/kommercheskaya-nedvizhimost/"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Региональные общие ссылки в меню Москва")
    public void shouldSeeMenuUrlsMoskvaAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(title).should(hasHref(equalTo(
                urlSteps.testing().path(AMP).path(MOSKVA).path(path).toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Региональные общие ссылки в меню Санкт-Петербург")
    public void shouldSeeMenuUrlsSanktPeterburgAmp() {
        urlSteps.testing().path(AMP).path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(title).should(hasHref(equalTo(
                urlSteps.testing().path(AMP).path(SANKT_PETERBURG).path(path).toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Региональные общие ссылки в меню Магадан")
    public void shouldSeeMenuUrlsMagadanAmp() {
        urlSteps.testing().path(AMP).path(MAGADAN).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(title).should(hasHref(equalTo(
                urlSteps.testing().path(AMP).path(MAGADAN).path(path).toString())));
    }
}
