package ru.yandex.realty.listing.specproject;

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
public class SamoletFiltersDeliveryDateTest {

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

                {"До 3 квартала 2022", "3_2022"},
                {"До 4 квартала 2022", "4_2022"},
                {"До 1 квартала 2023", "1_2023"},
                {"До 2 квартала 2023", "2_2023"},
                {"До 3 квартала 2023", "3_2023"},
                {"До 4 квартала 2023", "4_2023"},
                {"До 1 квартала 2024", "1_2024"},
                {"До 2 квартала 2024", "2_2024"},
                {"Сдан", "FINISHED"},
        });
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Параметр «срок сдачи»")
    public void shouldSeeDeliveryDateInUrl() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).open();
        user.onSamoletPage().searchFilters().button("Срок сдачи: неважно").click();
        user.onSamoletPage().popupWithItem().item(name).click();
        urlSteps.queryParam("deliveryDate", expected).shouldNotDiffWithWebDriverUrl();
    }
}
