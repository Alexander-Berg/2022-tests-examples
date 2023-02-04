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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.STATE_NEW;
import static ru.yandex.general.consts.Pages.STATE_USED;
import static ru.yandex.general.consts.QueryParams.NEW_VALUE;
import static ru.yandex.general.consts.QueryParams.OFFER_STATE_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_ASC_VALUE;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.consts.QueryParams.USED_VALUE;
import static ru.yandex.general.mobile.page.ListingPage.DONE;
import static ru.yandex.general.mobile.page.ListingPage.PRICE;
import static ru.yandex.general.mobile.page.ListingPage.PRICE_FROM;
import static ru.yandex.general.mobile.page.ListingPage.PRICE_TO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Фильтры на текстовом поиске")
@DisplayName("Фильтры на текстовом поиске")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class TextSearchFilterTest {

    private static final String TEXT_SEARCH = "ноутбук macbook";
    private static final String MIN_PRICE = "1000";
    private static final String MAX_PRICE = "200000";
    private static final String NOVIY = "Новый";
    private static final String BU = "Б/У";
    private static final String PRODUCER_NOUTBUKOV_ID = "offer.attributes.proizvoditel-noutbukov_vAeFtC";
    private static final String APPLE = "Apple";
    private static final String PRODUCER = "Производитель";
    private static final String OPERATION_MEMORY_ID = "offer.attributes.operativnaya-pamyat_15938685_serJvE";
    private static final String OPERATION_MEMORY = "Оперативная память";
    private static final String GB_8 = "8-gb";
    private static final String GB_16 = "16-gb";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.resize(375, 1500);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT_SEARCH);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «от» в попапе фильтров текстового поиска")
    public void shouldSeePriceFromInPopupTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(PRICE).waitUntil(isDisplayed());

        urlSteps.path(NOUTBUKI).queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «до» в попапе фильтров текстового поиска")
    public void shouldSeePriceToInPopupTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(PRICE).waitUntil(isDisplayed());

        urlSteps.path(NOUTBUKI).queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «от» и «до» в попапе фильтров текстового поиска")
    public void shouldSetPriceFromToInPopupTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(PRICE).waitUntil(isDisplayed());

        urlSteps.path(NOUTBUKI).queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фильтр «Б/у» в попапе фильтров текстового поиска")
    public void shouldSeeUsedFilterTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel(BU).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(BU).waitUntil(isDisplayed());

        urlSteps.path(NOUTBUKI).path(STATE_USED).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фильтр «Новый» в попапе фильтров текстового поиска")
    public void shouldSeeNewFilterTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(NOVIY).waitUntil(isDisplayed());

        urlSteps.path(NOUTBUKI).path(STATE_NEW).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фильтр по производителю в попапе фильтров текстового поиска")
    public void shouldSeeProducersFilterInPopupTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().inputWithFloatedPlaceholder(PRODUCER).click();
        basePageSteps.onListingPage().wrapper(PRODUCER).item(APPLE).click();
        basePageSteps.onListingPage().wrapper(PRODUCER).button("Готово").click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(PRODUCER).waitUntil(isDisplayed());

        urlSteps.path(NOUTBUKI).path("/proizvoditel-noutbukov-apple/").queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применение нескольких фильтров в попапе фильтров текстового поиска")
    public void shouldSeeSeveralFiltersInPopupTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().filters().checkboxWithLabel(NOVIY).click();

        basePageSteps.onListingPage().filters().inputWithFloatedPlaceholder(PRODUCER).click();
        basePageSteps.onListingPage().wrapper(PRODUCER).item(APPLE).click();
        basePageSteps.onListingPage().wrapper(PRODUCER).button(DONE).click();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().filters().inputWithFloatedPlaceholder(OPERATION_MEMORY).click();
        basePageSteps.onListingPage().wrapper(OPERATION_MEMORY).item("8 ГБ").click();
        basePageSteps.onListingPage().wrapper(OPERATION_MEMORY).item("16 ГБ").click();
        basePageSteps.onListingPage().wrapper(OPERATION_MEMORY).button(DONE).click();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().filters().checkboxWithLabel("Сначала дешевле").click();

        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(PRODUCER).waitUntil(isDisplayed());

        urlSteps.path(NOUTBUKI).path(STATE_NEW)
                .queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .queryParam(PRODUCER_NOUTBUKOV_ID, APPLE.toLowerCase())
                .queryParam(OPERATION_MEMORY_ID, GB_8)
                .queryParam(OPERATION_MEMORY_ID, GB_16)
                .queryParam(SORTING_PARAM, SORT_BY_PRICE_ASC_VALUE)
                .shouldNotDiffWithWebDriverUrl();

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем фильтры на текстовом поиске")
    public void shouldSeeResetFiltersTextSearch() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .queryParam(OFFER_STATE_PARAM, NEW_VALUE)
                .queryParam(PRODUCER_NOUTBUKOV_ID, APPLE.toLowerCase())
                .queryParam(OPERATION_MEMORY_ID, GB_8)
                .queryParam(OPERATION_MEMORY_ID, GB_16).open();

        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        basePageSteps.onListingPage().filters().cancel().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).queryParam(TEXT_PARAM, TEXT_SEARCH)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

}
