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
public class ExtendedFiltersBuildingClassTest {

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
    public String name;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> types() {
        return asList(new Object[][]{
                {"Эконом", "Эконом", "ECONOM"},
                {"Комфорт", "Комфорт", "COMFORT"},
                {"Комфорт плюс", "Комфорт +", "COMFORT_PLUS"},
                {"Бизнес", "Бизнес", "BUSINESS"},
                {"Элитный", "Элитный", "ELITE"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «класс здания»")
    public void shouldSeeClassesBuildingInUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().selectCheckBox(name);
        urlSteps.queryParam("buildingClass", expected).shouldNotDiffWithWebDriverUrl();
    }
}
