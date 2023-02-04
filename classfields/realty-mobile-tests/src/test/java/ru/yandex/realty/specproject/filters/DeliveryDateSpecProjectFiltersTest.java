package ru.yandex.realty.specproject.filters;

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
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Спецпроект. Фильтры")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DeliveryDateSpecProjectFiltersTest {

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
    public String expected;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"До 3 квартала 2022", "3_2022"},
                {"До 4 квартала 2022", "4_2022"},
                {"До 1 квартала 2023", "1_2023"},
                {"До 2 квартала 2023", "2_2023"},
                {"До 3 квартала 2023", "3_2023"},
                {"До 4 квартала 2023", "4_2023"},
                {"До 1 квартала 2024", "1_2024"},
                {"До 2 квартала 2024", "2_2024"},
                {"Сдан", "FINISHED"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «срок сдачи»")
    public void shouldSeeDeliveryDateInUrl() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSpecProjectPage().showFiltersButton().click();
        basePageSteps.onSpecProjectPage().filters().deliveryDateButton().click();
        basePageSteps.onSpecProjectPage().filters().deliveryDateOption(name).click();
        basePageSteps.onSpecProjectPage().filters().submitButton().click();
        urlSteps.queryParam("deliveryDate", expected).shouldNotDiffWithWebDriverUrl();
    }
}
