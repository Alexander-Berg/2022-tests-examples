package ru.yandex.realty.filters.geoselector;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.WithNewBuildingFilters.ADDRESS_INPUT;

@DisplayName("Фильтры: улица")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class StreetFiltersTest {

    private static final String TEST_STREET_NAME = "Тимуровская улица";
    private final static String STREET = "Арбат улица";
    private final static String STREET_PATH = "/st-ulica-arbat-62613/";
    private final static String UNIFIED_ADDRESS = "Россия%2C%20Москва%2C%20улица%20Арбат";

    private final static String NOT_CHPU_STREET = "Даниловский переулок";
    private final static String NOT_CHPU_UNIFIED_ADDRESS = "Россия, Москва, Даниловский переулок";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Адрес прокидыватся в фильтр")
    public void shouldSeeAddressInFilter() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA)
                .queryParam("unifiedAddress", TEST_STREET_NAME).open();
        basePageSteps.onOffersSearchPage().filters().badgesCounter().click();
        basePageSteps.onOffersSearchPage().filters().badges(TEST_STREET_NAME).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Адрес удаляется из урла")
    public void shouldNotSeeAddressInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA)
                .queryParam("unifiedAddress", TEST_STREET_NAME).open();
        basePageSteps.onOffersSearchPage().filters().badgesCounter().click();
        basePageSteps.onOffersSearchPage().filters().badges(TEST_STREET_NAME).clearGeo().click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Новостройки видим ЧПУ урл улиц")
    public void shouldSeeStreetChpuInUrl() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).open();
        basePageSteps.onNewBuildingPage().filters().input(ADDRESS_INPUT).sendKeys(STREET);
        basePageSteps.onNewBuildingPage().filters().suggest().waitUntil(hasSize(greaterThan(0))).get(0).click();

        basePageSteps.onNewBuildingPage().filters().submitButton().click();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .path(STREET_PATH).toString());
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Смешанная выдача видим ЧПУ урл улиц")
    public void shouldSeeListingStreetChpuInUrl() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().filters().geoInput().sendKeys(STREET);
        basePageSteps.onOffersSearchPage().filters().suggest().waitUntil(hasSize(greaterThan(0))).get(0).click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        basePageSteps.loaderWait();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA)
                .path(STREET_PATH).toString());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Смешанная выдача не видим ЧПУ урл улиц")
    public void shouldNotSeeStreetChpuInUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().filters().geoInput().sendKeys(NOT_CHPU_STREET);
        basePageSteps.onOffersSearchPage().filters().suggest().waitUntil(hasSize(greaterThan(0))).get(0).click();
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        basePageSteps.loaderWait();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA)
                .queryParam("unifiedAddress", NOT_CHPU_UNIFIED_ADDRESS).toString());
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Коттеджные поселки видим не ЧПУ урл улиц")
    public void shouldSeeVillagesStreetChpuInUrl() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onVillageListing().filters().input("Адрес, посёлок или Ж/Д станция").sendKeys(STREET);
        basePageSteps.onVillageListing().filters().suggest().waitUntil(hasSize(greaterThan(0))).get(0).click();

        basePageSteps.onVillageListing().filters().submitButton().click();
        urlSteps.shouldNotDiffWith(urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI)
                .queryParam("unifiedAddress", UNIFIED_ADDRESS).ignoreParam("rgid").toString());
    }
}
