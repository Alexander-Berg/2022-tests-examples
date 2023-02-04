package ru.yandex.general.suggest;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.GEO_SUGGEST_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.mobile.element.FiltersPopup.ADDRESS_AND_RADIUS;
import static ru.yandex.general.mobile.page.ListingPage.DONE;

@Epic(GEO_SUGGEST_FEATURE)
@DisplayName("Формирование URL при выборе адреса")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class AddressTest {

    private static final String ADDRESS = "Ленинградский проспект, 80к17";
    private static final String LATITUDE = "55.807953";
    private static final String LONGITUDE = "37.511509";
    private static final String RADIUS = "1000";
    private static final String RADIUS_MOVED = "20000";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе адреса")
    public void shouldSeeCoordinatesUrl() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(ADDRESS_AND_RADIUS).click();
        basePageSteps.onListingPage().addressSuggestScreen().findAddressInput().sendKeys(ADDRESS);
        basePageSteps.onListingPage().addressSuggestScreen().spanLink(ADDRESS).click();
        basePageSteps.onListingPage().wrapper(ADDRESS_AND_RADIUS).button(DONE).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.queryParam(LATITUDE_PARAM, LATITUDE)
                .queryParam(LONGITUDE_PARAM, LONGITUDE)
                .queryParam(GEO_RADIUS_PARAM, RADIUS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе адреса по крестику в инпуте")
    public void shouldSeeUrlAfterClearAddress() {
        urlSteps.queryParam(LATITUDE_PARAM, LATITUDE)
                .queryParam(LONGITUDE_PARAM, LONGITUDE)
                .queryParam(GEO_RADIUS_PARAM, RADIUS).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(ADDRESS_AND_RADIUS).clearInput().click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(MOSKVA).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе адреса из саджеста + смена радиуса")
    public void shouldSeeCoordinatesUrlAddressFromSuggestAndRadius() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(ADDRESS_AND_RADIUS).click();
        basePageSteps.onListingPage().addressSuggestScreen().findAddressInput().sendKeys(ADDRESS);
        basePageSteps.onListingPage().addressSuggestScreen().spanLink(ADDRESS).click();
        basePageSteps.onListingPage().wrapper(ADDRESS_AND_RADIUS).radiusSlider().click();
        basePageSteps.onListingPage().wrapper(ADDRESS_AND_RADIUS).button(DONE).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.queryParam(LATITUDE_PARAM, LATITUDE)
                .queryParam(LONGITUDE_PARAM, LONGITUDE)
                .queryParam(GEO_RADIUS_PARAM, RADIUS_MOVED).shouldNotDiffWithWebDriverUrl();
    }

}
