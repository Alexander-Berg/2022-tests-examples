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
public class ExtendedBuildingFiltersBuildingFloorTest {

    private static final String FLOOR = "Этажей в доме";

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
    @DisplayName("Параметр «этажей в доме от»")
    public void shouldSeeFloorMinInUrl() {
        String floorFrom = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onMapPage().extendFilters().byName(FLOOR).input("от").sendKeys(floorFrom + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam("minFloors", floorFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «этажей в доме до»")
    public void shouldSeeFloorMaxInUrl() {
        String floorTo = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onMapPage().extendFilters().byName(FLOOR).input("до").sendKeys(floorTo + Keys.ENTER);
        basePageSteps.loaderWait();
        urlSteps.queryParam("maxFloors", floorTo).shouldNotDiffWithWebDriverUrl();
    }
}
