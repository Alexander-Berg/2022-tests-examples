package ru.yandex.realty.filters.map.commercial;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Фильтры поиска по коммерческой недвижимости")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersFloorTest {

    private static final String FLOOR_MIN = "floorMin";
    private static final String FLOOR_MAX = "floorMax";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж»")
    public void shouldSeeFirstFloor() {
        basePageSteps.onMapPage().extendFilters().checkButton("Первый");
        basePageSteps.loaderWait();
        urlSteps.queryParam(FLOOR_MIN, "1").queryParam(FLOOR_MAX, "1").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж»")
    public void shouldSeeNotFirstFloor() {
        basePageSteps.onMapPage().extendFilters().checkButton("Выше первого");
        basePageSteps.loaderWait();
        urlSteps.queryParam(FLOOR_MIN, "2").shouldNotDiffWithWebDriverUrl();
    }
}
