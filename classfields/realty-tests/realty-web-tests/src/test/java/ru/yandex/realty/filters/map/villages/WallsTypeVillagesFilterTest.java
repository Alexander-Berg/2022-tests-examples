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
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Фильтр поиска по коттеджным поселкам.")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class WallsTypeVillagesFilterTest {

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
                {"Дерево", "WOOD"},
                {"Каркасно-щитовые", "FRAME"},
                {"Кирпич", "BRICK"},
                {"Фахверк", "TIMBER_FRAMING"},
                {"Бетон", "CONCRETE"},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Материал стен»")
    public void shouldSeeWallsType() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().select("Материал стен", name);
        basePageSteps.onMapPage().extendFilters().button(name).click();
        basePageSteps.onMapPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("wallsType", expected).shouldNotDiffWithWebDriverUrl();
    }
}
