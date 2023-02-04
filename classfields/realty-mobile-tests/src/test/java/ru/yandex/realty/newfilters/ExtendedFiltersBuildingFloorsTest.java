package ru.yandex.realty.newfilters;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Расширенные фильтры. Этаж")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ExtendedFiltersBuildingFloorsTest {

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
        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().extendFilters().byName(FLOOR));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр этажей в доме «от»")
    public void shouldSeeFloorMinInUrl() {
        String floorMin = valueOf(getRandomShortInt());
        basePageSteps.onMobileMainPage().extendFilters().byName(FLOOR).input("от").sendKeys(floorMin);
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();

        urlSteps.path(KUPIT).path(KVARTIRA).queryParam("minFloors", floorMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр этажей в доме «до»")
    public void shouldSeeFloorToInUrl() {
        String floorMax = valueOf(getRandomShortInt());
        basePageSteps.onMobileMainPage().extendFilters().byName(FLOOR).input("до").sendKeys(floorMax);
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();

        urlSteps.path(KUPIT).path(KVARTIRA).queryParam("maxFloors", floorMax).shouldNotDiffWithWebDriverUrl();
    }
}
