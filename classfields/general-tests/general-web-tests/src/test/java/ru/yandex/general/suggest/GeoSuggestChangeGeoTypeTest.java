package ru.yandex.general.suggest;

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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.GEO_SUGGEST_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.element.SuggestDropdown.MAP;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(GEO_SUGGEST_FEATURE)
@DisplayName("Формирование URL при смене типа гео")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class GeoSuggestChangeGeoTypeTest {

    private static final String ADDRESS = "Ленинградский проспект, 80к17";
    private static final String LATITUDE = "55.807953";
    private static final String LONGITUDE = "37.511509";
    private static final String RADIUS = "1000";
    private static final String PAVELECKAYA = "Павелецкая";
    private static final String PAVELECKAYA_ID = "20475";
    private static final String SILINO = "Силино";
    private static final String SILINO_ID = "116978";

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
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при смене адреса на метро")
    public void shouldSeeCoordinatesToSubwayChangeUrl() {
        basePageSteps.onListingPage().searchBar().fillSearchInput(ADDRESS);
        basePageSteps.onListingPage().searchBar().suggestItem(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().searchBar().suggest().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().searchBar().button(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station(PAVELECKAYA).click();
        basePageSteps.onListingPage().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));


        urlSteps.queryParam(METRO_ID_PARAM, PAVELECKAYA_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при смене адреса на район")
    public void shouldSeeCoordinatesToDistrictChangeUrl() {
        basePageSteps.onListingPage().searchBar().fillSearchInput(ADDRESS);
        basePageSteps.onListingPage().searchBar().suggestItem(ADDRESS).click();
        basePageSteps.onListingPage().button(SHOW).click();
        basePageSteps.onListingPage().searchBar().suggest().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().searchBar().button(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(SILINO).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(DISTRICT_ID_PARAM, SILINO_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при смене метро на адрес")
    public void shouldSeeSubwayToCoordinatesChangeUrl() {
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station(PAVELECKAYA).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().searchBar().suggest().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().searchBar().button(PAVELECKAYA).click();
        basePageSteps.onListingPage().searchBar().suggest().button(MAP).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(ADDRESS);
        basePageSteps.onListingPage().searchBar().suggestItem(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(LATITUDE_PARAM, LATITUDE)
                .queryParam(LONGITUDE_PARAM, LONGITUDE)
                .queryParam(GEO_RADIUS_PARAM, RADIUS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при смене района на метро")
    public void shouldSeeDistrictToSubwayChangeUrl() {
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(SILINO).click();
        basePageSteps.onListingPage().button(SHOW).click();
        basePageSteps.onListingPage().searchBar().suggest().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().searchBar().button(SILINO).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station(PAVELECKAYA).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(METRO_ID_PARAM, PAVELECKAYA_ID).shouldNotDiffWithWebDriverUrl();
    }

}
