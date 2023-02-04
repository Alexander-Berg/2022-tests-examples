package ru.yandex.realty.filters.map.offers;

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
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.util.Utils;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Расширенные фильтры поиска по объявлениям.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedApartmentFiltersFloorsTest {

    private static final String FLOOR = "Этаж";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж с»")
    public void shouldSeeFloorMinInUrl() {
        String floorFrom = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onMapPage().extendFilters().byName(FLOOR).input("c").sendKeys(floorFrom + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam("floorMin", floorFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Этаж по»")
    public void shouldSeeFloorMaxInUrl() {
        String floorTo = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onMapPage().extendFilters().byName(FLOOR).input("по").sendKeys(floorTo + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam("floorMax", floorTo).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Только последний этаж»")
    public void shouldSeeLastFloorInUrl() {
        basePageSteps.onMapPage().extendFilters().checkButton("Последний");
        basePageSteps.loaderWait();
        urlSteps.queryParam("lastFloor", "YES").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Только не последний этаж»")
    public void shouldSeeNotLastFloorInUrl() {
        basePageSteps.onMapPage().extendFilters().checkButton("Не последний");
        basePageSteps.loaderWait();
        urlSteps.queryParam("lastFloor", "NO").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Не первый этаж»")
    public void shouldSeeNotFirstFloorInUrl() {
        basePageSteps.onMapPage().extendFilters().checkButton("Не первый");
        basePageSteps.loaderWait();
        urlSteps.queryParam("floorExceptFirst", "YES").shouldNotDiffWithWebDriverUrl();
    }
}
