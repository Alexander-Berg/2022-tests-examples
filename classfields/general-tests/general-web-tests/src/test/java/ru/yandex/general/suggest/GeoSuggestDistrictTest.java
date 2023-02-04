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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.GEO_SUGGEST_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(GEO_SUGGEST_FEATURE)
@DisplayName("Формирование URL при выборе района")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class GeoSuggestDistrictTest {

    private static final String SILINO = "Силино";
    private static final String SILINO_ID = "116978";
    private static final String KAPOTNYA = "Капотня";
    private static final String KAPOTNYA_ID = "120545";

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
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе района")
    public void shouldSeeDistrictUrl() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(SILINO).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().searchBar().suggest().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(DISTRICT_ID_PARAM, SILINO_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе двух районов")
    public void shouldSeeTwoDistrictsUrl() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(SILINO).waitUntil(isDisplayed()).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(KAPOTNYA).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().searchBar().button(SHOW).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(DISTRICT_ID_PARAM, SILINO_ID)
                .queryParam(DISTRICT_ID_PARAM, KAPOTNYA_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при смене района")
    public void shouldSeeSecondDistrictsUrl() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(SILINO).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().searchBar().button(SILINO).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(SILINO).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(KAPOTNYA).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(DISTRICT_ID_PARAM, KAPOTNYA_ID).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе района по крестику в инпуте")
    public void shouldSeeUrlAfterClearDistrict() {
        urlSteps.queryParam(DISTRICT_ID_PARAM, SILINO_ID).open();
        basePageSteps.onListingPage().searchBar().clearGeoInput().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при сбросе района сняв чекбокс")
    public void shouldSeeUrlAfterClearDistrictInPopup() {
        urlSteps.queryParam(DISTRICT_ID_PARAM, SILINO_ID).open();
        basePageSteps.onListingPage().searchBar().button(SILINO).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(SILINO).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().searchBar().suggest().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

}
