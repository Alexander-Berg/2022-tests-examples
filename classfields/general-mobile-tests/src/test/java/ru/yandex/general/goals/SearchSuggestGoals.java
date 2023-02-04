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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_ADDRESS_CLICK;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_LOCATION_ITEM_CLICK;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_LOCATION_SHOW;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_MAP_RADIUS_CLICK;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_MAP_SHOW;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_METRO_SHOW;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_METRO_STATION_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.element.FiltersPopup.ADDRESS_AND_RADIUS;
import static ru.yandex.general.mobile.element.FiltersPopup.METRO;
import static ru.yandex.general.mobile.element.FiltersPopup.REGION;
import static ru.yandex.general.mobile.element.FiltersPopup.STATION_OR_LINE;
import static ru.yandex.general.mobile.page.ListingPage.DONE;
import static ru.yandex.general.mobile.page.ListingPage.SHOW_BUTTON;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на поисковом саджесте")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class SearchSuggestGoals {

    private static final String PAVELECKAYA = "Павелецкая";
    private static final String PROLETARSKAYA = "Пролетарская";
    private static final String SILINO = "Силино";
    private static final String KOPTEVO = "Коптево";
    private static final String ADDRESS = "Ленинградский проспект, 80к17";
    private static final String NIZHNIJ_NOVGOROD = "Нижний Новгород";

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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(ADDRESS_AND_RADIUS).click();
        basePageSteps.onListingPage().addressSuggestScreen().findAddressInput().sendKeys(ADDRESS);
        basePageSteps.onListingPage().addressSuggestScreen().spanLink(ADDRESS).click();

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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(ADDRESS_AND_RADIUS).click();
        basePageSteps.onListingPage().addressSuggestScreen().findAddressInput().sendKeys(ADDRESS);
        basePageSteps.onListingPage().addressSuggestScreen().spanLink(ADDRESS).click();
        basePageSteps.onListingPage().wrapper(ADDRESS_AND_RADIUS).radiusSlider().click();

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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(ADDRESS_AND_RADIUS).click();
        basePageSteps.onListingPage().addressSuggestScreen().findAddressInput().sendKeys(ADDRESS);
        basePageSteps.onListingPage().addressSuggestScreen().spanLink(ADDRESS).click();
        basePageSteps.onListingPage().wrapper(ADDRESS_AND_RADIUS).button(DONE).click();
        basePageSteps.onListingPage().filters().showOffers().click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_ADDRESS_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_METRO_SHOW)
    @DisplayName("Цель «SEARCH_SUGGEST_METRO_SHOW», при открытии списка станций метро")
    public void shouldSeeSearchSuggestMetroShow() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_METRO_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_METRO_STATION_CLICK)
    @DisplayName("Цель «SEARCH_SUGGEST_METRO_STATION_CLICK», при клике на станцию в списке")
    public void shouldSeeSearchSuggestMetroStationClick() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();
        basePageSteps.onListingPage().popup().spanLink(PROLETARSKAYA).click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();

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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();
        basePageSteps.onListingPage().popup().input(STATION_OR_LINE).sendKeys(PAVELECKAYA);
        basePageSteps.onListingPage().popup().spanLink(PAVELECKAYA).click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_METRO_STATION_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_LOCATION_SHOW)
    @DisplayName("Цель «SEARCH_SUGGEST_LOCATION_SHOW», при открытии выбора региона")
    public void shouldSeeSearchSuggestLocationShow() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(REGION).click();

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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(REGION).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup(REGION).spanLink("Россия").click();

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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(REGION).click();
        basePageSteps.onListingPage().popup(REGION).input().sendKeys(NIZHNIJ_NOVGOROD);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup(REGION).spanLink(NIZHNIJ_NOVGOROD).click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_LOCATION_ITEM_CLICK)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
