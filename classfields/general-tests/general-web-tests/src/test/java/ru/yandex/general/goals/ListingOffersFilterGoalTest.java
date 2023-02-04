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

import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_FILTER;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.QueryParams.NEW_VALUE;
import static ru.yandex.general.consts.QueryParams.OFFER_STATE_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;

@Epic(GOALS_FEATURE)
@Feature(LISTING_OFFERS_FILTER)
@DisplayName("Цель «LISTING_OFFERS_FILTER»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class ListingOffersFilterGoalTest {

    private static final String PRICE_FILTER_ID = "price";
    private static final String MANUFACTURER_FILTER_ID = "offer.attributes.proizvoditel-noutbukov_vAeFtC";
    private static final String PRICE_FROM_VALUE = "1";
    private static final String PRICE_TO_VALUE = "100000";
    private static final String NOVIY = "Новый";
    private static final String PROIZVODITEL_NOUTBUKOV_APPLE = "/proizvoditel-noutbukov-apple/";

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
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("1 цель «LISTING_OFFERS_FILTER» при выборе двух фильтров цены в попапе «Все фильтры»")
    public void shouldSeeOneGoalPriceFromAllFilters() {
        urlSteps.testing().path(NOUTBUKI).queryParam(PRICE_MIN_URL_PARAM, PRICE_FROM_VALUE)
                .queryParam(PRICE_MAX_URL_PARAM, PRICE_TO_VALUE).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().show();

        goalsSteps.withGoalType(LISTING_OFFERS_FILTER)
                .withCurrentPageRef()
                .withBody(goalRequestBody().setFilterId(PRICE_FILTER_ID))
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("1 цель «LISTING_OFFERS_FILTER» при выборе фильтра конечной категории в попапе «Все фильтры»")
    public void shouldSeeOneGoalCategoryFilterFromAllFilters() {
        urlSteps.testing().path(NOUTBUKI).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().checkboxWithLabel("Apple").click();
        basePageSteps.onListingPage().allFiltersPopup().show();

        goalsSteps.withGoalType(LISTING_OFFERS_FILTER)
                .withCurrentPageRef()
                .withBody(goalRequestBody().setFilterId(MANUFACTURER_FILTER_ID))
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("2 цели «LISTING_OFFERS_FILTER» при выборе двух разных фильтров в попапе «Все фильтры»")
    public void shouldSeeTwoGoalsFromAllFilters() {
        urlSteps.testing().path(NOUTBUKI).path(PROIZVODITEL_NOUTBUKOV_APPLE)
                .queryParam(OFFER_STATE_PARAM, NEW_VALUE).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().show();

        goalsSteps.withGoalType(LISTING_OFFERS_FILTER)
                .withCurrentPageRef()
                .withCount(2)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("3 цели «LISTING_OFFERS_FILTER» при сбросе трёх фильтров кнопкой «Сбросить все» в попапе «Все фильтры»")
    public void shouldSeeThreeGoalsFromAllFiltersReset() {
        urlSteps.testing().path(NOUTBUKI).path(PROIZVODITEL_NOUTBUKOV_APPLE)
                .queryParam(OFFER_STATE_PARAM, NEW_VALUE).queryParam(PRICE_MIN_URL_PARAM, PRICE_FROM_VALUE).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().cancel().click();
        basePageSteps.onListingPage().allFiltersPopup().show();

        goalsSteps.withGoalType(LISTING_OFFERS_FILTER)
                .withCurrentPageRef()
                .withCount(3)
                .shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("3 цели «LISTING_OFFERS_FILTER» при сбросе трёх фильтров поочередно в попапе «Все фильтры»")
    public void shouldSeeThreeGoalsFromAllFiltersResetByTurns() {
        urlSteps.testing().path(NOUTBUKI).path(PROIZVODITEL_NOUTBUKOV_APPLE)
                .queryParam(OFFER_STATE_PARAM, NEW_VALUE).queryParam(PRICE_MIN_URL_PARAM, PRICE_FROM_VALUE).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().spanLink(NOVIY).click();
        basePageSteps.onListingPage().allFiltersPopup().checkboxWithLabel("Apple").click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock("Цена").clearInput().click();
        basePageSteps.onListingPage().allFiltersPopup().show();

        goalsSteps.withGoalType(LISTING_OFFERS_FILTER)
                .withCurrentPageRef()
                .withCount(3)
                .shouldExist();
    }

}
