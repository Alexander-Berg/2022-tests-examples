package ru.yandex.realty.filters.mainpage;

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

import static java.lang.String.valueOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.TRYOHKOMNATNAYA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.DISTRICT;
import static ru.yandex.realty.element.saleads.FiltersBlock.HIGHWAY;
import static ru.yandex.realty.element.saleads.FiltersBlock.METRO;
import static ru.yandex.realty.element.saleads.FiltersBlock.NEWBUILDINGS_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.PRICE_FROM;
import static ru.yandex.realty.element.saleads.FiltersBlock.VIEW_ON_MAP;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;
import static ru.yandex.realty.step.UrlSteps.PRICE_MIN_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.ROOMS_TOTAL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.SITE_NAME;
import static ru.yandex.realty.utils.UtilsWeb.getNormalPrice;

/**
 * @author kantemirov
 */
@DisplayName("Главная страница. Базовые фильтры.")
@Feature(MAINFILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BaseFiltersSearchTest {

    private static final String ADDRESS = "Прудовой проезд, 9к1";
    private static final String URL_ADDRESS = "Россия, Москва, Прудовой проезд, 9к1";
    private static final String COMPLEX = "Бунинские луга";
    private static final String COMPLEX_PATH = "/zhk-buninskie-luga-166185/";
    private static final String TESTING_DISTRICT_NAME = "Крылатское";
    private static final String TESTING_DISTRICT_PATH = "/dist-krylatskoe-193383/";
    private static final String TESTING_METRO_NAME = "Университет";
    private static final String TESTING_METRO_PATH = "/metro-universitet/";
    private static final String TEST_HIGHWAY = "Можайское шоссе";
    private static final String TEST_HIGHWAY_PATH = "/shosse-mozhajskoe/";
    private final static String STREET_CHPU = "Арбат улица";
    private final static String STREET_CHPU_PATH = "/st-ulica-arbat-62613/";
    private static final String NEW_BUILDING_NAME = "Саларьево парк";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(MOSKVA_I_MO).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по улице")
    public void shouldSeeStreetSearchInUrl() {
        basePageSteps.onMainPage().filters().geoInput().sendKeys(ADDRESS);
        basePageSteps.onMainPage().filters().suggest().get(0).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).queryParam("unifiedAddress", URL_ADDRESS)
                .shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по улице чпу")
    public void shouldSeeChpuStreetSearchInUrl() {
        basePageSteps.onMainPage().filters().geoInput().sendKeys(STREET_CHPU);
        basePageSteps.onMainPage().filters().suggest().waitUntil(hasSize(greaterThan(0))).get(0).click();
        basePageSteps.onMainPage().filters().submitButton().click();
        basePageSteps.loaderWait();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA)
                .path(STREET_CHPU_PATH).toString());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по ЖК")
    public void shouldSeeComplexInUrl() {
        basePageSteps.onMainPage().filters().geoInput().sendKeys(COMPLEX);
        basePageSteps.onMainPage().filters().suggest().get(0).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path(COMPLEX_PATH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по району")
    public void shouldSeeDistrictNameInUrl() {
        basePageSteps.onMainPage().filters().geoButtons().spanLink(DISTRICT).click();
        basePageSteps.onMainPage().geoSelectorPopup().selectCheckBox(TESTING_DISTRICT_NAME);
        basePageSteps.onMainPage().geoSelectorPopup().submitButton().click();
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path(TESTING_DISTRICT_PATH)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по шоссе")
    public void shouldSeeHighwayInUrl() {
        basePageSteps.onMainPage().filters().geoButtons().spanLink(HIGHWAY).click();
        basePageSteps.onMainPage().geoSelectorPopup().selectCheckBox(TEST_HIGHWAY);
        basePageSteps.onMainPage().geoSelectorPopup().submitButton().click();
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path(TEST_HIGHWAY_PATH).shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по метро")
    public void shouldSeeMetroStationInUrl() {
        basePageSteps.onOffersSearchPage().filters().geoButtons().spanLink(METRO).click();
        basePageSteps.onOffersSearchPage().geoSelectorPopup().tab(METRO).click();
        basePageSteps.onMainPage().geoSelectorPopup().metroSuggest().input().sendKeys(TESTING_METRO_NAME);
        basePageSteps.onMainPage().geoSelectorPopup().metroSuggest().suggestListItem(TESTING_METRO_NAME).click();
        basePageSteps.onMainPage().geoSelectorPopup().submitButton().click();
        basePageSteps.onMainPage().filters().submitButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path(TESTING_METRO_PATH)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по карте. Цена и комнатность")
    public void shouldSeePriceAndRoomsInMapUrl() {
        basePageSteps.onMainPage().filters().selectCheckBox("3");
        String priceMin = valueOf(getNormalPrice());
        basePageSteps.onMainPage().filters().price().input(PRICE_FROM).sendKeys(priceMin);
        basePageSteps.onMainPage().filters().link(VIEW_ON_MAP).click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(TRYOHKOMNATNAYA).path(KARTA)
                .queryParam(PRICE_MIN_URL_PARAM, priceMin)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Поиск по карте. Цена и комнатность")
    public void shouldSeeNewBuildingRoomsInMapUrl() {
        basePageSteps.onMainPage().filters().selectButton(NEWBUILDINGS_BUTTON);
        basePageSteps.onMainPage().filters().selectCheckBox("4+");
        basePageSteps.onMainPage().filters().selectCheckBox("Студия");
        basePageSteps.onMainPage().filters().geoInput().sendKeys(NEW_BUILDING_NAME);
        basePageSteps.onMainPage().filters().suggest().get(0).waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().filters().link(VIEW_ON_MAP).click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).path(KARTA)
                .queryParam(ROOMS_TOTAL_URL_PARAM, "PLUS_4", "STUDIO").queryParam("siteId", "375274")
                .queryParam("from", "index_nb_sites").queryParam(SITE_NAME, NEW_BUILDING_NAME)
                .shouldNotDiffWithWebDriverUrl();
    }
}
