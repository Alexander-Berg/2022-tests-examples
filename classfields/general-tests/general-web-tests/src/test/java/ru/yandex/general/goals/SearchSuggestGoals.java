package ru.yandex.general.goals;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_ADDRESS_CLICK;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_DISTRICTS_ITEM_CLICK;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_DISTRICTS_SHOW;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_LOCATION_ITEM_CLICK;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_LOCATION_SHOW;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_MAP_RADIUS_CLICK;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_MAP_SHOW;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_METRO_SHOW;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_METRO_STATION_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.general.element.SuggestDropdown.RADIUS;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на поисковом саджесте")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class SearchSuggestGoals {

    private static final String PAVELECKAYA = "Павелецкая";
    private static final String SILINO = "Силино";
    private static final String ADDRESS = "Ленинградский проспект, 80к17";
    private static final String SANKT_PETERBURG = "Санкт-Петербург";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_MAP_SHOW)
    @DisplayName("Цель «SEARCH_SUGGEST_MAP_SHOW», при открытии карты")
    public void shouldSeeSearchSuggestMapShow() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_MAP_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_MAP_RADIUS_CLICK)
    @DisplayName("Цель «SEARCH_SUGGEST_MAP_RADIUS_CLICK», при смене радиуса")
    public void shouldSeeSearchSuggestMapRadiusClick() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().spanLink(RADIUS).click();
        basePageSteps.onListingPage().searchBar().suggest().radiusSlider().click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_MAP_RADIUS_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_ADDRESS_CLICK)
    @DisplayName("Цель «SEARCH_SUGGEST_ADDRESS_CLICK», при выборе адреса из саджеста")
    public void shouldSeeSearchSuggestAddressClick() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(ADDRESS);
        basePageSteps.onListingPage().searchBar().suggestItem(ADDRESS).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_ADDRESS_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_METRO_SHOW)
    @DisplayName("Цель «SEARCH_SUGGEST_METRO_SHOW», при открытии схемы метро")
    public void shouldSeeSearchSuggestMetroShow() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_METRO_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_METRO_STATION_CLICK)
    @DisplayName("Цель «SEARCH_SUGGEST_METRO_STATION_CLICK», при клике на станцию на схеме")
    public void shouldSeeSearchSuggestMetroStationClick() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station(PAVELECKAYA).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_METRO_STATION_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_METRO_STATION_CLICK)
    @DisplayName("Цель «SEARCH_SUGGEST_METRO_STATION_CLICK», при выборе метро из саджеста")
    public void shouldSeeSearchSuggestMetroStationClickFromSuggest() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(PAVELECKAYA);
        basePageSteps.onListingPage().searchBar().suggestItem(PAVELECKAYA).click();
        basePageSteps.onListingPage().button(SHOW).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_METRO_STATION_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_DISTRICTS_SHOW)
    @DisplayName("Цель «SEARCH_SUGGEST_DISTRICTS_SHOW», при открытии списка районов")
    public void shouldSeeSearchSuggestDistrictsShow() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_DISTRICTS_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_DISTRICTS_ITEM_CLICK)
    @DisplayName("Цель «SEARCH_SUGGEST_DISTRICTS_ITEM_CLICK», при выборе района")
    public void shouldSeeSearchSuggestDistrictsItemClick() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel(SILINO).click();
        basePageSteps.onListingPage().button(SHOW).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_DISTRICTS_ITEM_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_DISTRICTS_ITEM_CLICK)
    @DisplayName("Цель «SEARCH_SUGGEST_DISTRICTS_ITEM_CLICK», при выборе района из саджеста")
    public void shouldSeeSearchSuggestDistrictsItemClickFromSuggest() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput(SILINO);
        basePageSteps.onListingPage().searchBar().suggestItem(SILINO).click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_DISTRICTS_ITEM_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_LOCATION_SHOW)
    @DisplayName("Цель «SEARCH_SUGGEST_LOCATION_SHOW», при открытии выбора региона")
    public void shouldSeeSearchSuggestLocationShow() {
        basePageSteps.onListingPage().region().click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_LOCATION_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_LOCATION_ITEM_CLICK)
    @DisplayName("Цель «SEARCH_SUGGEST_LOCATION_ITEM_CLICK», при выборе региона из предложенных")
    public void shouldSeeSearchSuggestLocationItemClick() {
        basePageSteps.onListingPage().region().click();
        basePageSteps.onListingPage().searchBar().suggest().spanLink("Россия").click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_LOCATION_ITEM_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_LOCATION_ITEM_CLICK)
    @DisplayName("Цель «SEARCH_SUGGEST_LOCATION_ITEM_CLICK», при выборе региона из саджеста")
    public void shouldSeeSearchSuggestLocationItemClickFromSuggest() {
        basePageSteps.onListingPage().region().click();
        basePageSteps.onListingPage().searchBar().input().sendKeys(SANKT_PETERBURG);
        basePageSteps.onListingPage().searchBar().suggestItem(SANKT_PETERBURG).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_LOCATION_ITEM_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
