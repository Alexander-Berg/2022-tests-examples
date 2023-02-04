package ru.yandex.realty.filters.map.badges;

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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Гео Фильтры: город")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CityBadgeTest {

    private static final String CITY_NAME = "Санкт-Петербург";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Вводим город меняется регион")
    public void shouldSeeAddressInUrl() {
        basePageSteps.onMapPage().filters().geoInput().sendKeys(CITY_NAME);
        basePageSteps.onMapPage().filters().suggest().waitUntil(hasSize(greaterThan(0))).get(0).click();
        basePageSteps.loaderWait();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA)
                .toString());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Бэйджик не появляется после ввода города")
    public void shouldNotSeeBadgeInFilters() {
        basePageSteps.onMapPage().filters().geoInput().sendKeys(CITY_NAME);
        basePageSteps.onMapPage().filters().suggest().get(0).click();
        basePageSteps.loaderWait();
        basePageSteps.onMapPage().filters().badgesCounter().should(not(exists()));
    }
}
