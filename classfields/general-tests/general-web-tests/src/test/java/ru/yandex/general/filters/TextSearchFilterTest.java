package ru.yandex.general.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.QueryParams.NEW_VALUE;
import static ru.yandex.general.consts.QueryParams.OFFER_STATE_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.consts.QueryParams.USED_VALUE;
import static ru.yandex.general.page.ListingPage.CONDITION;
import static ru.yandex.general.page.ListingPage.PRICE;
import static ru.yandex.general.page.ListingPage.PRICE_FROM;
import static ru.yandex.general.page.ListingPage.PRICE_TO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Фильтры на текстовом поиске")
@DisplayName("Фильтры на текстовом поиске")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
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
    private static final String GB_8 = "8-gb";
    private static final String GB_4 = "4-gb";
    private static final String OPERATION_MEMORY = "Оперативная память";

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
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT_SEARCH);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «от» в попапе фильтров текстового поиска")
    public void shouldSeePriceFromInPopupTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «до» в попапе фильтров текстового поиска")
    public void shouldSeePriceToInPopupTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цена «от» и «до» в попапе фильтров текстового поиска")
    public void shouldSetPriceFromToInPopupTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фильтр «Новый» в попапе фильтров текстового поиска")
    public void shouldSeeNewFilterTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().filters().counter().should(hasText("1"));
        urlSteps.queryParam(OFFER_STATE_PARAM, NEW_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фильтр «Б/у» в попапе фильтров текстового поиска")
    public void shouldSeeUsedFilterTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(BU).click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().filters().counter().should(hasText("1"));
        urlSteps.queryParam(OFFER_STATE_PARAM, USED_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фильтр по производителю в попапе фильтров текстового поиска")
    public void shouldSeeProducersFilterInPopupTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRODUCER).checkboxWithLabel(APPLE)
                .click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(PRODUCER_NOUTBUKOV_ID, APPLE.toLowerCase()).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применение нескольких фильтров в попапе фильтров текстового поиска")
    public void shouldSeeSeveralFiltersInPopupTextSearch() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRODUCER).checkboxWithLabel(APPLE)
                .click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(OPERATION_MEMORY).checkboxWithLabel("4 ГБ")
                .click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(OPERATION_MEMORY).checkboxWithLabel("8 ГБ")
                .click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .queryParam(OFFER_STATE_PARAM, NEW_VALUE)
                .queryParam(PRODUCER_NOUTBUKOV_ID, APPLE.toLowerCase())
                .queryParam(OPERATION_MEMORY_ID, GB_4)
                .queryParam(OPERATION_MEMORY_ID, GB_8)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем фильтры на текстовом поиске")
    public void shouldSeeResetFiltersTextSearch() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .queryParam(OFFER_STATE_PARAM, NEW_VALUE)
                .queryParam(PRODUCER_NOUTBUKOV_ID, APPLE.toLowerCase())
                .queryParam(OPERATION_MEMORY_ID, "16-gb")
                .queryParam(OPERATION_MEMORY_ID, "8-gb").open();

        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().cancel().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT_SEARCH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применяем фильтры в попапе фильтров, затем сбрасываем их кнопкой -> фильтры не применились")
    public void shouldSeeCancelChosedFiltersInPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRODUCER).checkboxWithLabel(APPLE)
                .click();
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        basePageSteps.onListingPage().allFiltersPopup().cancel().click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().filters().counter().should(CoreMatchers.not(isDisplayed()));
        basePageSteps.onListingPage().filters().chipsList().should(hasSize(0));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применяем фильтры в попапе фильтров, затем закрываем попап -> фильтры не применились")
    public void shouldSeeCancelChosedFiltersInPopupByClosingPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRODUCER).checkboxWithLabel(APPLE)
                .click();
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        basePageSteps.onListingPage().allFiltersPopup().close().click();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().filters().counter().should(CoreMatchers.not(isDisplayed()));
        basePageSteps.onListingPage().filters().chipsList().should(hasSize(0));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Вводим цену «от» в попапе фильтров, очищаем её, применяем фильтры -> фильтра по цене нет")
    public void shouldSeeNoPriceFilterAfterClearPriceInputInPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).clearInput().click();
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().filters().counter().should(CoreMatchers.not(isDisplayed()));
        basePageSteps.onListingPage().filters().chipsList().should(hasSize(0));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

}
