package ru.yandex.realty.filters.map.villages;

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
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Фильтр поиска по коттеджным поселкам.")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DirectionDistanceMaxVillagesFilterTest {

    private static final String DIRECTION_DISTANCE_MAX = "directionDistanceMax";

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

    @Parameterized.Parameters(name = "{index} -{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"До 5 км", "5"},
                {"До 10 км", "10"},
                {"До 15 км", "15"},
                {"До 20 км", "20"},
                {"До 30 км", "30"},
                {"До 40 км", "40"},
                {"До 50 км", "50"},
                {"До 60 км", "60"},
                {"До 70 км", "70"},
                {"До 80 км", "80"},
                {"До 90 км", "90"},
                {"До 100 км", "100"},
                {"До 150 км", "150"},
                {"До 200 км", "200"},
                {"До 250 км", "250"},
                {"До 300 км", "300"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Удаленность от Москвы»")
    public void shouldSeeDirectionDistanceMax() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().select("Удаленность от Москвы", name);
        basePageSteps.onMapPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(DIRECTION_DISTANCE_MAX, expected).shouldNotDiffWithWebDriverUrl();
    }
}
