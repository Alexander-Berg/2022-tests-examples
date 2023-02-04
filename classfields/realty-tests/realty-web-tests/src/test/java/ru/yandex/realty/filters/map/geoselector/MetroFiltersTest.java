package ru.yandex.realty.filters.map.geoselector;

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
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.ODNOKOMNATNAYA;
import static ru.yandex.realty.consts.Location.MOSCOW_OBL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.METRO;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;

@DisplayName("Карта. Фильтры: метро")
@Feature(MAPFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MetroFiltersTest {

    private final String TESTING_METRO_NAME = "Курская";
    private final String TESTING_METRO_PATH = "/metro-kurskaya/";
    private final String TESTING_METRO_PARAM = "20479";
    private final String SECOND_TESTING_METRO_NAME = "Бауманская";
    private final String SECOND_TESTING_METRO_PARAM = "20478";
    private final String TESTING_METRO_LINE_NAME = "Калининская линия";
    private final List<String> TESTING_METRO_LINE_PARAMS =
            newArrayList("20390", "20391", "20392", "20408", "20468", "20476", "20477", "114781");

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE)).createWithDefaults();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().filters().geoButtons().spanLink(METRO).click();
        basePageSteps.onMapPage().geoSelectorPopup().waitUntil(isDisplayed());
        basePageSteps.onMapPage().geoSelectorPopup().tab("Метро").click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим название метро в урле")
    public void shouldSeeMetroStationInUrl() {
        basePageSteps.onMapPage().geoSelectorPopup().metroSuggest().input().sendKeys(TESTING_METRO_NAME);
        basePageSteps.onMapPage().geoSelectorPopup().metroSuggest().suggestListItem(TESTING_METRO_NAME).click();
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(TESTING_METRO_PATH).path(KARTA)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим, что два метро переносятся в параметры, а не путь в урле")
    public void shouldSeeTwoMetroStationsInUrl() {
        basePageSteps.onMapPage().geoSelectorPopup().metroSuggest().input().sendKeys(TESTING_METRO_NAME);
        basePageSteps.onMapPage().geoSelectorPopup().metroSuggest().suggestListItem(TESTING_METRO_NAME).click();
        basePageSteps.onMapPage().geoSelectorPopup().metroSuggest().input().sendKeys(SECOND_TESTING_METRO_NAME);
        basePageSteps.onMapPage().geoSelectorPopup().metroSuggest().suggestListItem(SECOND_TESTING_METRO_NAME).click();
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.loaderWait();
        urlSteps.queryParam("metroGeoId", TESTING_METRO_PARAM)
                .queryParam("metroGeoId", SECOND_TESTING_METRO_PARAM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим линию метро в урле")
    public void shouldSeeMetroLineInUrl() {
        basePageSteps.onMapPage().geoSelectorPopup().metroLineButton().click();
        basePageSteps.onMapPage().selectPopup().item(TESTING_METRO_LINE_NAME).click();
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.loaderWait();
        TESTING_METRO_LINE_PARAMS.forEach(param -> urlSteps.queryParam("metroGeoId", param));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим название метро и количество комнат в урле")
    public void shouldSeeMetroStationAndRoomTotalParamInUrl() {
        basePageSteps.onMapPage().geoSelectorPopup().metroSuggest().input().sendKeys(TESTING_METRO_NAME);
        basePageSteps.onMapPage().geoSelectorPopup().metroSuggest().suggestListItem(TESTING_METRO_NAME).click();
        basePageSteps.onMapPage().geoSelectorPopup().submitButton().click();
        basePageSteps.loaderWait();

        basePageSteps.onMapPage().filters().checkButton("1");
        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(ODNOKOMNATNAYA)
                .path(TESTING_METRO_PATH).path(KARTA).shouldNotDiffWithWebDriverUrl();
    }
}
