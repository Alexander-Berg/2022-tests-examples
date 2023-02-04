package ru.yandex.realty.filters.map.newbuilding;

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

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Расширенные фильтры поиска по новостройкам.")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersAreaTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь от»")
    public void shouldSeeAreaMinInUrl() {
        String areaFrom = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onMapPage().extendFilters().byName("Площадь").input("от").sendKeys(areaFrom + Keys.ENTER);
        urlSteps.queryParam("areaMin", areaFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь до»")
    public void shouldSeeAreaMaxInUrl() {
        String areaTo = String.valueOf(Utils.getRandomShortInt());
        basePageSteps.onMapPage().extendFilters().byName("Площадь").input("до").sendKeys(areaTo + Keys.ENTER);
        urlSteps.queryParam("areaMax", areaTo).shouldNotDiffWithWebDriverUrl();
    }
}
