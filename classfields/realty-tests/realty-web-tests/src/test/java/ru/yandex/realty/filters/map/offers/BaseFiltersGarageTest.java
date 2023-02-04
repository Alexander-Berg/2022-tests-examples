package ru.yandex.realty.filters.map.offers;

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
import static ru.yandex.realty.consts.Filters.GARAZH;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.TYPE_BUTTON;

@DisplayName("Карта. Базовые фильтры поиска по объявлениям")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersGarageTest {

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
                {"Бокс", "BOX"},
                {"Гараж", "GARAGE"},
                {"Машиноместо", "PARKING_PLACE"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(GARAZH).path(KARTA).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «типа гаража»")
    public void shouldSeeBuyTypeOfGarageUrl() {
        basePageSteps.onMapPage().filters().button(TYPE_BUTTON).click();
        basePageSteps.onMapPage().filters().selectPopup().item(label).click();
        basePageSteps.loaderWait();
        urlSteps.queryParam("garageType", expected).shouldNotDiffWithWebDriverUrl();
    }
}
