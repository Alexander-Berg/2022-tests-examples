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
import static ru.yandex.realty.consts.Owners.CHERNOV;
import static ru.yandex.realty.consts.RealtyFeatures.MENU;

@Issue("Меню. Спецпроекты")
@Feature(MENU)
@DisplayName("Региональные спецпроекты в меню")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MenuSpecialProjectTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String region;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"moskva", "/samolet/"},
                {"tatarstan", "/tatarstan/zastroyschik/unistroj-463788/"},
                {"sankt-peterburg", "/sankt-peterburg_i_leningradskaya_oblast/samolet/"},
                {"udmurtiya", "/udmurtiya/zastroyschik/komosstroj-896162/"},
                {"rostovskaya_oblast", "/rostovskaya_oblast/zastroyschik/yugstrojinvest-230663/"}
        });
    }

    @Test
    @Owner(CHERNOV)
    @DisplayName("Меню. Переход на страницу спецпроекта")
    public void shouldOpenSamoletLanding() {
        urlSteps.testing().path(region).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().specialProject().click();
        urlSteps.testing().path(url).shouldNotDiffWithWebDriverUrl();
    }
}
