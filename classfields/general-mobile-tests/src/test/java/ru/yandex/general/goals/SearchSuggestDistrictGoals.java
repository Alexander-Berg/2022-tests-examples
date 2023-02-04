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

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_DISTRICTS_ITEM_CLICK;
import static ru.yandex.general.consts.Goals.SEARCH_SUGGEST_DISTRICTS_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CHELYABINSK;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.mobile.element.FiltersPopup.DISTRICT_NAME;
import static ru.yandex.general.mobile.page.ListingPage.SHOW_BUTTON;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на поисковом саджесте")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class SearchSuggestDistrictGoals {

    private static final String TRAKTOROZAVODSKIY = "Тракторозаводский район";
    private static final String LENINSKIY = "Ленинский район";

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
        urlSteps.testing().path(CHELYABINSK).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(SEARCH_SUGGEST_DISTRICTS_SHOW)
    @DisplayName("Цель «SEARCH_SUGGEST_DISTRICTS_SHOW», при открытии списка районов")
    public void shouldSeeSearchSuggestDistrictsShow() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).click();

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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).click();
        basePageSteps.onListingPage().popup().spanLink(LENINSKIY).click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();

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
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(DISTRICT).click();
        basePageSteps.onListingPage().popup().input(DISTRICT_NAME).sendKeys(TRAKTOROZAVODSKIY);
        basePageSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        basePageSteps.onListingPage().popup().spanLink(TRAKTOROZAVODSKIY).click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();

        goalsSteps.withGoalType(SEARCH_SUGGEST_DISTRICTS_ITEM_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

}
