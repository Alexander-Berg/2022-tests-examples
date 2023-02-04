package ru.yandex.realty.filters.map.offers;

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
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.realty.consts.Filters.GARAZH;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.step.BasePageSteps.urlParam;

@DisplayName("Карта. Расширенные фильтры поиска по объявлениям.")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersRentGarageComfortTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"КУ", "hasUtilitiesIncluded"},
                {"Электроэнергия", "hasElectricityIncluded"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр удобств")
    public void shouldSeeComfortFiltersInUrl() {
        urlSteps.testing().path(MOSKVA_I_MO).path(SNYAT).path(GARAZH).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().checkButton(label);
        basePageSteps.loaderWait();
        urlSteps.queryParam(expected, UrlSteps.YES_VALUE).shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam(expected, "YES")));
    }
}
