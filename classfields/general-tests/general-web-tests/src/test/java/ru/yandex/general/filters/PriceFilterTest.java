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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.element.FilterChip.RESET_ALL;
import static ru.yandex.general.page.ListingPage.APPLY;
import static ru.yandex.general.page.ListingPage.PRICE_FROM;
import static ru.yandex.general.page.ListingPage.PRICE_TO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Фильтры цены")
@DisplayName("Фильтры. Цена")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class PriceFilterTest {

    private static final String MIN_PRICE = "1";
    private static final String MAX_PRICE = "100000";
    private static final String PRICE = "Цена";
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
        price = String.valueOf(nextInt(1, 1000));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Очищаем «цена от» и применяем -> не применяется фильтр")
    public void shouldNotSeePriceToFilter() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(price);
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).clearInput().click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Закрываем попап с введенной «ценой от» -> не применяется фильтр")
    public void shouldSeeClosePopupPrice() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(price);
        basePageSteps.onListingPage().allFiltersPopup().closeAllFiltersPopupButton().click();
        basePageSteps.waitSomething(500, TimeUnit.MILLISECONDS);

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Скидываем фильтр цены")
    public void shouldSeeCloseFilter() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, price).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).clearInput().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().filters().counter().should(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Цена «от» устанавливается через попап фильтров в урле")
    public void shouldSeePriceFromInPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(price);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().filters().chips(format("Цена: от %s", price)).should(isDisplayed());
        basePageSteps.onListingPage().filters().counter().should(hasText("1"));
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, price).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Цена «до» устанавливается через попап фильтров в урле")
    public void shouldSeePriceToInPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().filters().chips("Цена: до 100 000").should(isDisplayed());
        basePageSteps.onListingPage().filters().counter().should(hasText("1"));
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Цена «от» и «до» устанавливается через попап фильтров в урле")
    public void shouldSetPriceFromToInPopup() {
        urlSteps.open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().filters().chips("Цена: 1 – 100 000").should(isDisplayed());
        basePageSteps.onListingPage().filters().counter().should(hasText("1"));
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Цена «от - до» отображается в попапе расширенных фильтров при переходе по урлу")
    public void shouldSeePriceFromToInPopup() {
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).open();
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).should(hasValue(MIN_PRICE));
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).should(hasValue("100 000"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем фильтр по цене «от - до» по клику на чипсину «Сбросить все»")
    public void shouldSeeCancelPriceFilterByResetAllChips() {
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).open();
        basePageSteps.onListingPage().filters().chips(RESET_ALL).click();

        basePageSteps.onListingPage().filters().counter().should(not(isDisplayed()));
        basePageSteps.onListingPage().filters().chipsList().should(hasSize(0));
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем фильтр по цене «от» по клику на крестик в чипсине цены")
    public void shouldSeeCancelPriceFilterByResetButtonInChips() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).open();
        basePageSteps.onListingPage().filters().chips(PRICE).reset().click();

        basePageSteps.onListingPage().filters().counter().should(not(isDisplayed()));
        basePageSteps.onListingPage().filters().chipsList().should(hasSize(0));
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем фильтр по цене «от» в попапе чипсины")
    public void shouldSeeResetPriceFromChipsPopup() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).open();
        basePageSteps.onListingPage().filters().chips(PRICE).click();
        basePageSteps.onListingPage().popup().input("от").clearInput().click();
        basePageSteps.onListingPage().popup().button(APPLY).click();

        basePageSteps.onListingPage().filters().counter().should(not(isDisplayed()));
        basePageSteps.onListingPage().filters().chipsList().should(hasSize(0));
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем фильтр по цене «от» в попапе по клику на чипсину")
    public void shouldSeeChangePriceFromInChipsPopup() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).open();
        basePageSteps.onListingPage().filters().chips(PRICE).click();
        basePageSteps.onListingPage().popup().input("от").clearInput().click();
        basePageSteps.onListingPage().popup().input("от").sendKeys("100");
        basePageSteps.onListingPage().popup().button(APPLY).click();

        basePageSteps.onListingPage().filters().chips("Цена: от 100").should(isDisplayed());
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).queryParam(PRICE_MIN_URL_PARAM, "100")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по цене «до» к фильтру «от» в попапе по клику на чипсину")
    public void shouldAddToPriceFromInChipsPopup() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).open();
        basePageSteps.onListingPage().filters().chips(PRICE).click();
        basePageSteps.onListingPage().popup().input("до").sendKeys(MAX_PRICE);
        basePageSteps.onListingPage().popup().button(APPLY).click();

        basePageSteps.onListingPage().filters().chips("Цена: 1 – 100 000").should(isDisplayed());
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по цене «до» и очищаем «от» в попапе по клику на чипсину")
    public void shouldAddToPriceAndClearFromInChipsPopup() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).open();
        basePageSteps.onListingPage().filters().chips(PRICE).click();
        basePageSteps.onListingPage().popup().input("от").clearInput().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onListingPage().popup().input("до").sendKeys(MAX_PRICE);
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onListingPage().popup().button(APPLY).click();

        basePageSteps.onListingPage().filters().chips("Цена: до 100 000").should(isDisplayed());
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очищаем «от и до» в попапе по клику на чипсину, отменяем изменения - не применяются")
    public void shouldSeeResetPriceFilterAndCancelIt() {
        urlSteps.queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE).open();
        basePageSteps.onListingPage().filters().chips(PRICE).click();
        basePageSteps.onListingPage().popup().input("от").clearInput().click();
        basePageSteps.onListingPage().popup().input("до").clearInput().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().button("Отмена").click();

        basePageSteps.onListingPage().popup().should(not(isDisplayed()));
        basePageSteps.onListingPage().filters().chips("Цена: 1 – 100 000").should(isDisplayed());
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

}
