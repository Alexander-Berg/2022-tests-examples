package ru.yandex.realty.filters.map.newbuilding;

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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.DeliveryDate.getItemForPlusQuarter;
import static ru.yandex.realty.consts.DeliveryDate.getRelativeUrlParam;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Расширенные фильтры поиска по новостройкам.")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersDeliveryDateTest {

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

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {getItemForPlusQuarter(0), getRelativeUrlParam(0)},
                {getItemForPlusQuarter(1), getRelativeUrlParam(1)},
                {getItemForPlusQuarter(2), getRelativeUrlParam(2)},
                {getItemForPlusQuarter(3), getRelativeUrlParam(3)},
                {getItemForPlusQuarter(4), getRelativeUrlParam(4)},
                {getItemForPlusQuarter(5), getRelativeUrlParam(5)},
                {getItemForPlusQuarter(6), getRelativeUrlParam(6)},
                {getItemForPlusQuarter(7), getRelativeUrlParam(7)},
                {getItemForPlusQuarter(7), getRelativeUrlParam(7)},
                {"Сдан", "FINISHED"},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «срок сдачи»")
    public void shouldSeeDeliveryTypeInUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.onMapPage().filters().waitUntil(isDisplayed());
        basePageSteps.onMapPage().filters().select("Срок сдачи", name);
        urlSteps.queryParam("deliveryDate", expected).shouldNotDiffWithWebDriverUrl();
    }
}
