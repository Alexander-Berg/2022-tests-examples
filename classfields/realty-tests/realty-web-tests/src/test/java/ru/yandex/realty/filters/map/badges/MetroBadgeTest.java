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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Гео Фильтры: метро")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MetroBadgeTest {

    private static final String METRO_NAME = "Академическая";
    private static final String METRO_PATH = "metro-akademicheskaya-1/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Метро появляется в урле после поиска")
    public void shouldSeeAddressInUrl() {
        urlSteps.path(KARTA).open();

        basePageSteps.onMapPage().filters().geoInput().sendKeys(METRO_NAME);
        basePageSteps.onMapPage().filters().suggest().get(0).click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(METRO_PATH).path(KARTA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Появляется бэйджик после ввода метро")
    public void shouldSeeBadgeInFilters() {
        urlSteps.open();

        basePageSteps.onMapPage().filters().geoInput().sendKeys(METRO_NAME);
        basePageSteps.onMapPage().filters().suggest().get(0).click();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.loaderWait();
        basePageSteps.onMapPage().filters().badges(METRO_NAME).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Появляется бэйджик после перехода на урл с метро")
    public void shouldSeeBadgeFromUrl() {
        urlSteps.path(METRO_PATH).path(KARTA).open();
        basePageSteps.onMapPage().filters().badgesCounter().click();
        basePageSteps.loaderWait();
        basePageSteps.onMapPage().filters().badges(METRO_NAME).should(isDisplayed());
    }
}
