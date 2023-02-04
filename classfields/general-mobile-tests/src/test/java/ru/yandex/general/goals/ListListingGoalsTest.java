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

import static ru.yandex.general.beans.GoalRequestBody.goalRequestBody;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SORTING_FEATURE;
import static ru.yandex.general.consts.Goals.BREADCRUMBS_CATEGORY_CLICK;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_FILTER;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_SHOW;
import static ru.yandex.general.consts.Goals.LISTING_OFFERS_SORT_PRICE_ASC_CLICK;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mobile.page.ListingPage.PRICE;
import static ru.yandex.general.mobile.page.ListingPage.PRICE_FROM;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.LIST;

@Epic(GOALS_FEATURE)
@DisplayName("Цели на листинге списком")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class ListListingGoalsTest {

    private static final String TEXT = "ноутбук";
    private static final String PRICE_FILTER_ID = "price";
    private static final String PRICE_FROM_VALUE = "1";

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
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);
    }

    @Test
    @Feature(LISTING_OFFERS_SHOW)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «LISTING_OFFERS_SHOW» при открытии категории, листинг списком")
    public void shouldSeeListingOffersShowGoalOnCategoryListListing() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();

        goalsSteps.withGoalType(LISTING_OFFERS_SHOW)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Feature(LISTING_OFFERS_SHOW)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет цели «LISTING_OFFERS_SHOW» при текстовом поиске по категории, листинг списком")
    public void shouldNotSeeListingOffersShowGoalOnOpenTextSearhListListing() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).queryParam(TEXT_PARAM, TEXT).open();

        goalsSteps.withGoalType(LISTING_OFFERS_SHOW)
                .withCount(0)
                .shouldExist();
    }

    @Test
    @Feature(LISTING_OFFERS_FILTER)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «LISTING_OFFERS_FILTER» при вводе фильтра цены «От», листинг списком")
    public void shouldSeePriceGoalFromFilterListListing() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_FROM)
                .sendKeys(PRICE_FROM_VALUE);
        basePageSteps.onListingPage().filters().showOffers().click();

        goalsSteps.withGoalType(LISTING_OFFERS_FILTER)
                .withCurrentPageRef()
                .withBody(goalRequestBody().setFilterId(PRICE_FILTER_ID))
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Feature(SORTING_FEATURE)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отправка цели сортировки «Сначала дешевле», листинг списком")
    public void shouldSeeSortGoalListListing() {
        basePageSteps.resize(600, 1500);
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().spanLink("Сначала дешевле").click();
        basePageSteps.onListingPage().filters().showOffers().click();

        goalsSteps.withGoalType(LISTING_OFFERS_SORT_PRICE_ASC_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }

    @Test
    @Feature(BREADCRUMBS_CATEGORY_CLICK)
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «BREADCRUMBS_CATEGORY_CLICK» при переходе по ХК на листинге, листинг списком")
    public void shouldSeeBreadcrumbsCategoryClickGoalFromListListing() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).open();
        basePageSteps.onListingPage().breadcrumbsItem("Компьютерная техника").hover().click();

        goalsSteps.withGoalType(BREADCRUMBS_CATEGORY_CLICK)
                .withPageRef(urlSteps.toString())
                .withCount(1)
                .shouldExist();
    }
}
