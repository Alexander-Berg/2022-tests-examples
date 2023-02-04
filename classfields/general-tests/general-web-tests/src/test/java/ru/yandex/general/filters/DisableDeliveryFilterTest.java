package ru.yandex.general.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FILTERS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.DISABLE_DELIVERY;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.ROSSIYA;
import static ru.yandex.general.consts.Pages.STATE_NEW;
import static ru.yandex.general.consts.QueryParams.DISABLE_DELIVERY_PARAM;
import static ru.yandex.general.consts.QueryParams.FALSE_VALUE;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_PRICE_ASC_VALUE;
import static ru.yandex.general.consts.QueryParams.TRUE_VALUE;
import static ru.yandex.general.element.FilterChip.RESET_ALL;
import static ru.yandex.general.element.Filters.FIND_IN_MY_REGION;
import static ru.yandex.general.page.ListingPage.CONDITION;
import static ru.yandex.general.page.ListingPage.PRICE;
import static ru.yandex.general.page.ListingPage.PRICE_FROM;
import static ru.yandex.general.page.ListingPage.PRICE_TO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FILTERS_FEATURE)
@Feature("Чекбокс «Искать в моем регионе»")
@DisplayName("Тесты на чекбокс «Искать в моем регионе»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class DisableDeliveryFilterTest {

    private static final String SANKT_PETERBURG = "Санкт-Петербург";
    private static final String LENINGRADSKAYA_OBLAST = "/leningradskaya_oblast/";
    private static final String MAX_PRICE = "300000";
    private static final String MIN_PRICE = "1";

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
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Активируем чекбокс «Искать в моем регионе»")
    public void shouldActivateCheckboxFindInMyRegion() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).open();
        basePageSteps.onListingPage().filters().checkboxWithLabel(FIND_IN_MY_REGION).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.path(DISABLE_DELIVERY).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Деактивируем чекбокс «Искать в моем регионе»")
    public void shouldDeactivateCheckboxFindInMyRegion() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).path(DISABLE_DELIVERY).open();
        basePageSteps.onListingPage().filters().checkboxWithLabel(FIND_IN_MY_REGION).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).queryParam(DISABLE_DELIVERY_PARAM, FALSE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Для региона «Россия» не отображается чекбокс «Искать в моем регионе»")
    public void shouldSeeNoCheckboxFindInMyRegionInRussia() {
        urlSteps.testing().path(ROSSIYA).path(NOUTBUKI).open();

        basePageSteps.onListingPage().filters().checkboxWithLabel(FIND_IN_MY_REGION).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("При смене региона фильтрация «Искать в моем регионе» деактивируется")
    public void shouldSeeDeactivateFiltrationFindInMyRegionAfterChangeRegion() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).open();
        basePageSteps.onListingPage().filters().checkboxWithLabel(FIND_IN_MY_REGION).click();
        basePageSteps.onListingPage().region().click();
        basePageSteps.onListingPage().searchBar().input().sendKeys(SANKT_PETERBURG);
        basePageSteps.onListingPage().searchBar().suggestItem(SANKT_PETERBURG).click();

        urlSteps.testing().path(LENINGRADSKAYA_OBLAST).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("В попапе всех фильтров нет «Искать в моем регионе»")
    public void shouldSeeNoFindInMyRegionInAllFiltersPopup() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).open();
        basePageSteps.onListingPage().openExtFilter();

        basePageSteps.onListingPage().allFiltersPopup().filterBlock(FIND_IN_MY_REGION).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применяем 3 фильтра, активируем «Искать в моем регионе», проверяем URL")
    public void shouldSeeUrlAfterSetThreeFiltersAndFindInMyRegionChecbox() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).open();
        fillFilters();
        basePageSteps.onListingPage().filters().checkboxWithLabel(FIND_IN_MY_REGION).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.path(STATE_NEW).queryParam(DISABLE_DELIVERY_PARAM, TRUE_VALUE)
                .queryParam("offer.attributes.operativnaya-pamyat_15938685_serJvE", "8-gb")
                .queryParam(PRICE_MIN_URL_PARAM, MIN_PRICE)
                .queryParam(PRICE_MAX_URL_PARAM, MAX_PRICE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применяем 3 фильтра, активируем «Искать в моем регионе», проверяем чипсины фильтров")
    public void shouldSeeChipsAfterSetThreeFiltersAndFindInMyRegionChecbox() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).open();
        fillFilters();
        basePageSteps.onListingPage().filters().checkboxWithLabel(FIND_IN_MY_REGION).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        basePageSteps.onListingPage().filters().chips("Цена: 1 – 300 000").should(isDisplayed());
        basePageSteps.onListingPage().filters().chips("Оперативная память: 8 ГБ").should(isDisplayed());
        basePageSteps.onListingPage().filters().chips("Состояние: Новый").should(isDisplayed());
        basePageSteps.onListingPage().filters().counter().should(hasText("3"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Применяем 3 фильтра, активируем «Искать в моем регионе», жмем «Сбросить все», фильтр по региону сбрасывается")
    public void shouldSeeResetAllAfterSetThreeFiltersAndFindInMyRegionChecbox() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).open();
        fillFilters();
        basePageSteps.onListingPage().filters().checkboxWithLabel(FIND_IN_MY_REGION).click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().filters().chips(RESET_ALL).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Issue("CLASSFRONT-2033")
    @Owner(ALEKS_IVANOV)
    @DisplayName("Устанавливаем сортировку, активируем чекбокс «Искать в моем регионе», проверяем URL")
    public void shouldSeeUrlActivateCheckboxFindInMyRegionAfterSort() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).open();
        basePageSteps.onListingPage().filters().sortButton().click();
        basePageSteps.onListingPage().popup().radioButtonWithLabel("Сначала дешевле").click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onListingPage().filters().checkboxWithLabel(FIND_IN_MY_REGION).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        urlSteps.path(DISABLE_DELIVERY).queryParam(SORTING_PARAM, SORT_BY_PRICE_ASC_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Активируем чекбокс «Искать в моем регионе», не добавляется каунтер к фильтрам")
    public void shouldSeeNoCounterAfterActivateCheckboxFindInMyRegion() {
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).open();
        basePageSteps.onListingPage().filters().checkboxWithLabel(FIND_IN_MY_REGION).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        basePageSteps.onListingPage().filters().counter().should(not(isDisplayed()));
    }

    private void fillFilters() {
        basePageSteps.onListingPage().openExtFilter();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_FROM).sendKeys(MIN_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(PRICE).input(PRICE_TO).sendKeys(MAX_PRICE);
        basePageSteps.onListingPage().allFiltersPopup().filterBlock(CONDITION).checkboxWithLabel("Новый").click();
        basePageSteps.onListingPage().allFiltersPopup().filterBlock("Оперативная память").checkboxWithLabel("8 ГБ")
                .click();
        basePageSteps.onListingPage().allFiltersPopup().show();
        basePageSteps.onListingPage().skeleton().waitUntil(Matchers.not(isDisplayed()));
    }

}
