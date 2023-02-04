package ru.yandex.realty.listing.specproject;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
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
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.Pages.CATALOG;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Лендинг Самолета. Фильтры")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SamoletFiltersRoomTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Студия", "/studiya/"},
                {"1", "/odnokomnatnaya/"},
                {"2", "/dvuhkomnatnaya/"},
                {"3", "/tryohkomnatnaya/"},
                {"4", "/4-i-bolee/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).open();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Параметр «количество комнат»")
    public void shouldSeeRoomTotalParamInUrl() {
        user.onSamoletPage().searchFilters().button(name).click();
        urlSteps.path(expected).shouldNotDiffWithWebDriverUrl();
    }
}
