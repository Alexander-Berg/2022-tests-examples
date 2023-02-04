package ru.yandex.realty.filters.map.geoselector;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.METRO;


@DisplayName("Карта. Фильтры: метро")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MetroFiltersTransportTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String transport;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"Пешком", "ON_FOOT"},
                {"На транспорте", "ON_TRANSPORT"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onOffersSearchPage().filters().geoButtons().spanLink(METRO).click();
        basePageSteps.onOffersSearchPage().geoSelectorPopup().waitUntil(isDisplayed());
        basePageSteps.onOffersSearchPage().geoSelectorPopup().tab("Метро").click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим время до метро и тип транспорта в урле")
    public void shouldSeeTimeToMetroInUrl() {
        basePageSteps.onMapPage().geoSelectorPopup().timeToMetroInput().click();
        basePageSteps.onMapPage().selectPopup().item("До 30 мин").waitUntil(isDisplayed()).click();
        basePageSteps.onMapPage().geoSelectorPopup().metroTransport().click();
        basePageSteps.onMapPage().selectPopup().item(transport).waitUntil(isDisplayed()).click();
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.loaderWait();
        urlSteps.queryParam("timeToMetro", "30").queryParam("metroTransport", expected)
                .shouldNotDiffWithWebDriverUrl();
    }
}
