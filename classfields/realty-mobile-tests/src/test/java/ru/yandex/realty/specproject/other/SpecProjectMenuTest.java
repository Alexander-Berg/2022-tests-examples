package ru.yandex.realty.specproject.other;

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
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.MOBILE;

@DisplayName("Спецпроект.")
@Feature(MOBILE)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SpecProjectMenuTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String section;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"Проекты", "/catalog/"},
                {"Акции", "/discounts/"},
                {"Планировки", "/plans/"},
                {"Ипотека\u00a05,99%", "/mortgage/"},
                {"О\u00a0компании", "/about/"},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход по страницам меню Спецпроекта")
    public void shouldSeeSpecProjMenuSection() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSpecProjectPage().specProjMenu().click();
        basePageSteps.onSpecProjectPage().specProjMenu()
                .waitUntil(hasClass(not(containsString("SamoletMenu__collapsed"))));
        basePageSteps.onSpecProjectPage().specProjMenu().section(section).click();
        basePageSteps.onSpecProjectPage().specProjMenu().waitUntil(hasClass(containsString("SamoletMenu__collapsed")));
        urlSteps.testing().path(SAMOLET).path(path).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переход по страницам футера Спецпроекта")
    public void shouldSeeSpecProjBottomSection() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSpecProjectPage().specProjBottom().section(section).click();
        urlSteps.path(path).shouldNotDiffWithWebDriverUrl();
    }
}
