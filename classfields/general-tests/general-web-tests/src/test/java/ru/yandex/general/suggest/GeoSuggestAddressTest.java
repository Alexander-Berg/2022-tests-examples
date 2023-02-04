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
import org.openqa.selenium.interactions.Actions;
import ru.yandex.general.element.SuggestDropdown;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.GEO_SUGGEST_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(GEO_SUGGEST_FEATURE)
@DisplayName("Формирование URL при выборе адреса")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class GeoSuggestAddressTest {

    private static final String ADDRESS = "Ленинградский проспект, 80к17";
    private static final String LATITUDE = "55.807953";
    private static final String LONGITUDE = "37.511509";
    private static final String LATITUDE_MOVED = "55.772572";
    private static final String LONGITUDE_MOVED = "37.588172";
    private static final String LATITUDE_DEFAULT = "55.753215";
    private static final String LONGITUDE_DEFAULT = "37.622504";
    private static final String ADDRESS_2 = "Сущёвская улица, 21с5";
    private static final String LATITUDE_2 = "55.782677";
    private static final String LONGITUDE_2 = "37.600822";
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
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе адреса")
    public void shouldSeeCoordinatesUrl() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
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
    @DisplayName("Формирование URL при смене адреса")
    public void shouldSeeSecondCoordinatesUrl() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(ADDRESS);
        basePageSteps.onListingPage().searchBar().suggestItem(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().searchBar().suggest().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().searchBar().button(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(ADDRESS_2);
        basePageSteps.onListingPage().searchBar().suggestItem(ADDRESS_2).click();
        basePageSteps.onListingPage().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(LATITUDE_PARAM, LATITUDE_2)
                .queryParam(LONGITUDE_PARAM, LONGITUDE_2)
                .queryParam(GEO_RADIUS_PARAM, RADIUS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе адреса по крестику в инпуте")
    public void shouldSeeUrlAfterClearAddress() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(LATITUDE_PARAM, LATITUDE_2)
                .queryParam(LONGITUDE_PARAM, LONGITUDE_2)
                .queryParam(GEO_RADIUS_PARAM, RADIUS).open();
        basePageSteps.onListingPage().searchBar().clearGeoInput().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL - двигаем пин")
    public void shouldSeeCoordinatesUrlMovePin() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.wait500MS();
        movePin();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(LATITUDE_PARAM, LATITUDE_MOVED)
                .queryParam(LONGITUDE_PARAM, LONGITUDE_MOVED)
                .queryParam(GEO_RADIUS_PARAM, RADIUS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL - двигаем пин + смена радиуса")
    public void shouldSeeCoordinatesUrlMovePinAndRaius() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.wait500MS();
        movePin();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onListingPage().searchBar().suggest().spanLink(SuggestDropdown.RADIUS).click();
        basePageSteps.onListingPage().searchBar().suggest().radiusSlider().click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(LATITUDE_PARAM, LATITUDE_MOVED)
                .queryParam(LONGITUDE_PARAM, LONGITUDE_MOVED)
                .queryParam(GEO_RADIUS_PARAM, RADIUS_MOVED).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL - дефолтный центр Москвы + смена радиуса")
    public void shouldSeeCoordinatesUrlMoveRaius() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().searchBar().suggest().spanLink(SuggestDropdown.RADIUS).click();
        basePageSteps.onListingPage().searchBar().suggest().radiusSlider().click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(LATITUDE_PARAM, LATITUDE_DEFAULT)
                .queryParam(LONGITUDE_PARAM, LONGITUDE_DEFAULT)
                .queryParam(GEO_RADIUS_PARAM, RADIUS_MOVED).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе адреса из саджеста + смена радиуса")
    public void shouldSeeCoordinatesUrlAddressSuggestAndRadius() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(ADDRESS);
        basePageSteps.onListingPage().searchBar().suggestItem(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().suggest().spanLink(SuggestDropdown.RADIUS).click();
        basePageSteps.onListingPage().searchBar().suggest().radiusSlider().click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(LATITUDE_PARAM, LATITUDE)
                .queryParam(LONGITUDE_PARAM, LONGITUDE)
                .queryParam(GEO_RADIUS_PARAM, RADIUS_MOVED).shouldNotDiffWithWebDriverUrl();
    }

    private void movePin() {
        Actions action = new Actions(basePageSteps.getDriver());
        action.moveToElement(basePageSteps.onListingPage().map(), 60, 10)
                .clickAndHold().moveByOffset(100, 100).release().build().perform();
    }

}
