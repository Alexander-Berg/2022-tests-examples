package ru.yandex.realty.filters.newbuilding;

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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KNYAZKIN;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;


@DisplayName("Базовые фильтры поиска по новостройкам. Срок сдачи")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)

@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersDeliveryDateChpuTest {

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
                {"До 4 квартала 2022", "sdacha-2022/"},
                {"До 4 квартала 2023", "sdacha-2023/"},
                {"Сдан", "s-kluchami/"},
        });
    }

    @Before
    public void openNewBuildingPage() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).open();
        user.onNewBuildingPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KNYAZKIN)
    @DisplayName("Параметр «срок сдачи»")
    public void shouldSeeDeliveryTypeInUrl() {
        user.onNewBuildingPage().filters().select("Срок сдачи", name);
        user.onNewBuildingPage().filters().submitButton().click();
        urlSteps.path(expected).shouldNotDiffWithWebDriverUrl();
    }
}
