package ru.yandex.general.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.QueryParams.MOSCOW_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.REGION_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.mobile.page.ListingPage.PRICE;
import static ru.yandex.general.mobile.page.ListingPage.PRICE_FROM;
import static ru.yandex.general.mobile.page.ListingPage.PRICE_TO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Фильтры цены")
@DisplayName("Фильтры. Цена")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class PriceFilterTest {

    private String price;

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
        price = String.valueOf(nextInt(1, 9));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтры «цена от»")
    public void shouldSeePriceFromFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_FROM).sendKeys(price);
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(format("%s %s", PRICE_FROM, price)).waitUntil(isDisplayed());
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, price).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтры «цена до»")
    public void shouldSeePriceToFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_TO).sendKeys(price);
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(format("%s %s", PRICE_TO, price)).waitUntil(isDisplayed());
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, price).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Фильтры «цена от до»")
    public void shouldSeePriceFromToFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_TO).sendKeys(price);
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_FROM).sendKeys(price);
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(format("%s – %s", price, price)).waitUntil(isDisplayed());
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, price).queryParam(PRICE_MIN_URL_PARAM, price)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Очищаем «цена до» и применяем -> не применяется фильтр")
    public void shouldNotSeePriceToFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_TO).sendKeys(price);
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_TO).clearInput().click();
        basePageSteps.onListingPage().filters().showOffers().click();
        urlSteps.queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Отменяем «цена от» -> не применияется фильтр")
    public void shouldSeeCancelPrice() {
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_FROM).sendKeys(price);
        basePageSteps.onListingPage().filters().closePopup().click();
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Видим зачеканный фильтр «от» при переходе по урлу")
    public void shouldSeeCheckedPriceFrom() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, price).open();
        basePageSteps.onListingPage().filter(format("%s %s", PRICE_FROM, price)).should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Видим зачеканный фильтр «до» при переходе по урлу")
    public void shouldSeeCheckedPriceTo() {
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, price).open();
        basePageSteps.onListingPage().filter(format("%s %s", PRICE_TO, price)).should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Видим зачеканный фильтр «от до» при переходе по урлу")
    public void shouldSeeCheckedPriceFromTo() {
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, price).queryParam(PRICE_MIN_URL_PARAM, price).open();
        basePageSteps.onListingPage().filter(format("%s – %s", price, price)).should(isDisplayed());
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Скидываем фильтр цены")
    public void shouldSeeCloseFilter() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, price).open();
        basePageSteps.onListingPage().filter(format("%s %s", PRICE_FROM, price)).closeFilter().click();
        basePageSteps.wait500MS();
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Ignore("Пока не починят на фронте")
    @Owner(ILUHA)
    @Description("НАДО ЗАМОКАТЬ")
    @DisplayName("При переходе по фильтрованному урлу отображаются не все фильтры -> сбрасываем -> " +
            "должны быть все фильтры")
    public void shouldSeeAllCategories() {
        urlSteps.testing().path(NOUTBUKI).queryParam(PRICE_MAX_URL_PARAM, price).queryParam(PRICE_MIN_URL_PARAM, price)
                .queryParam(REGION_ID_PARAM, MOSCOW_ID_VALUE).open();
        basePageSteps.onListingPage().filter(format("%s – %s", price, price)).closeFilter().click();
        basePageSteps.onListingPage().filter("Емкость аккумулятора  от – до").should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Устанавливаем фильтр цены в попапе фильтров, затем меняем в чипсине")
    public void shouldSeeChangePriceInChips() {
        String priceDigit = "3";
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_TO).sendKeys(price);
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(format("%s %s", PRICE_TO, price)).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().wrapper(PRICE).inputWithFloatedPlaceholder(PRICE_TO).sendKeys(priceDigit);
        basePageSteps.onListingPage().wrapper(PRICE).button("Найти объявления").click();

        basePageSteps.onListingPage().filter(format("%s %s%s", PRICE_TO, price, priceDigit)).should(isDisplayed());
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, format("%s%s", price, priceDigit))
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Устанавливаем фильтр цены «от» в попапе фильтров, затем добавляем «до» в чипсине")
    public void shouldSeeSetPriceToChips() {
        String priceChips = "1";
        urlSteps.open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_TO).sendKeys(price);
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().filter(format("%s %s", PRICE_TO, price)).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().wrapper(PRICE).inputWithFloatedPlaceholder(PRICE_FROM).sendKeys(priceChips);
        basePageSteps.onListingPage().wrapper(PRICE).button("Найти объявления").click();
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);

        basePageSteps.onListingPage().filter(format("%s – %s", priceChips, price)).should(isDisplayed());
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, price)
                .queryParam(PRICE_MIN_URL_PARAM, priceChips)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

}
