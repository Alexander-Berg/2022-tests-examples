package ru.yandex.general.filters;

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
import ru.yandex.general.consts.Pages;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.page.ListingPage.FREE;
import static ru.yandex.general.page.ListingPage.PRICE;
import static ru.yandex.general.page.ListingPage.PRICE_FROM;
import static ru.yandex.general.page.ListingPage.PRICE_TO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Фильтры цены")
@DisplayName("Фильтры. Цена")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FiltersFreePriceWithFromToTest {

    private static final String MIN_PRICE = "1";
    private static final String MAX_PRICE = "100000";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «от» сбрасывается в попапе при выборе цены «Даром»")
    public void shouldSeeResetFromPrice() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(FREE).switcher().click();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).should(hasValue(""));
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.path(Pages.FREE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «до» сбрасывается в попапе при выборе цены «Даром»")
    public void shouldSeeResetToPrice() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(FREE).switcher().click();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).should(hasValue(""));
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.path(Pages.FREE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цены «от» и «до» сбрасываются в попапе при выборе цены «Даром»")
    public void shouldSeeResetFromToPrice() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(FREE).switcher().click();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).should(hasValue(""));
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).should(hasValue(""));
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.path(Pages.FREE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «Даром» сбрасывается в попапе при выборе цены «от»")
    public void shouldSeeResetFreeByFromPrice() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(FREE).switcher().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);

        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «Даром» сбрасывается в попапе при выборе цены «до»")
    public void shouldSeeResetFreeByToPrice() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(FREE).switcher().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «от» сбрасывается в попапе при выборе цены «Даром», открываем по прямому урлу")
    public void shouldSeeResetFromPriceDirectLink() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(FREE).switcher().click();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).should(hasValue(""));
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).path(Pages.FREE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «до» сбрасывается в попапе при выборе цены «Даром», открываем по прямому урлу")
    public void shouldSeeResetToPriceDirectLink() {
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(FREE).switcher().click();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).should(hasValue(""));
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).path(Pages.FREE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цены «от» и «до» сбрасываются в попапе при выборе цены «Даром», открываем по прямому урлу")
    public void shouldSeeResetFromToPriceDirectLink() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(FREE).switcher().click();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).should(hasValue(""));
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).should(hasValue(""));
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).path(Pages.FREE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «Даром» сбрасывается в попапе при выборе цены «от», открываем по прямому урлу")
    public void shouldSeeResetFreeByFromPriceDirectLink() {
        urlSteps.path(Pages.FREE).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «Даром» сбрасывается в попапе при выборе цены «до», открываем по прямому урлу")
    public void shouldSeeResetFreeByToPriceDirectLink() {
        urlSteps.path(Pages.FREE).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE)
                .shouldNotDiffWithWebDriverUrl();
    }

}